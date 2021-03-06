package com.appcloud.vm.action.shot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import aliyun.openapi.client.AliSnapshotClient;
import com.aliyuncs.ecs.model.v20140526.DescribeSnapshotsResponse;
import com.appcloud.vm.fe.manager.AppkeyManager;
import com.appcloud.vm.util.CollectionUtil;
import com.appcloud.vm.util.StringUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;


import appcloud.openapi.client.ImageClient;
import appcloud.openapi.client.InstanceClient;
import appcloud.openapi.client.SnapshotClient;
import appcloud.openapi.client.VolumeClient;
import appcloud.openapi.datatype.DiskDetailItem;
import appcloud.openapi.datatype.InstanceAttributes;
import appcloud.openapi.datatype.SnapshotDetailItem;
import appcloud.openapi.response.DescribeInstancesResponse;
import appcloud.openapi.response.DisksDetailReponse;
import appcloud.openapi.response.SnapshotsDetailReponse;

import com.appcloud.vm.action.BaseAction;
import com.appcloud.vm.fe.model.Appkey;
import com.appcloud.vm.fe.util.OpenClientFactory;
import com.appcloud.vm.fe.common.Constants;
import com.appcloud.vm.fe.common.TransformAttribute;
import com.appcloud.vm.fe.entity.ShotStatus;
import org.apache.log4j.Logger;

/**
 * 云快照列表页面，将其重构于（2016-07-22），每次的页面不会一次性获取所有的Appkey，
 * 所有的获取操作均需要传递provider参数，将原本在声明在类成员变量中的变量放到对应函数中私有变量
 *
 * @author 包鑫彤
 * @create 2016-07-22-14:40
 * @since 1.0.0
 */
public class ShotListAction extends BaseAction {
    private static final long serialVersionUID = 1L;
    private Logger LOGGER = Logger.getLogger(ShotListAction.class);

    private Integer userId = this.getSessionUserID();
    private AppkeyManager appkeyManager = new AppkeyManager();

    private String provider;
    private String regionId;
    private String zoneId;
    private String appname;

    //自定义的实体类用于包装返回的数据 本来需要对不同的厂商写不同的类，如果两者数据 一样则可以使用同一个 类
    private List<ShotStatus> shotServers = new ArrayList<ShotStatus>();
    
    //快照分页参数
    private double TotalCount;//云快照总个数
    private String totalPage = "1";//云快照总的页数
    private Integer PageSize = 10;
    private String page = "1";
    private String souType = "all";

    //云海快照列表及其搜索传递的参数
    private HashMap<Integer, String> zoneIdNameMap = new HashMap<Integer, String>();
    private TransformAttribute transform = new TransformAttribute();

    private String skeyword = null;
    private static String shotDiskType = null;
    private String result = "0";//0:表示正确，1:参数出错

    //阿里云快照列表及其搜索传递的参数
    private String aliSourceDiskType;
    private String aliYunSearchType;
    private String aliYunKeyWord;


    /**
     * 根据厂商和regionId来获取 快照列表
     * @return 获取的列表数据为 jsp页面，具体请到 配置文件中 查看 shot.xml
     */
    public String shotlist() {
        LOGGER.info(toString());
        Appkey appkey = appkeyManager.getAppkeyByUserIdAndAppName(userId, appname);
        provider= appkey.getProvider();
        switch (appkey.getProvider()) {
            case Constants.YUN_HAI:
                LOGGER.info("开始获取云海的快照列表");
                return YunHaiSnapShots(appkey);

            case Constants.ALI_YUN:
                LOGGER.info("开始获取阿里云的快照列表");
                return AliYunSnapShots(appkey);

            case Constants.AMAZON:
                LOGGER.info("开始获取亚马逊的快照列表");
                return addAmazonShot(appkey, shotServers);
        }
        LOGGER.info(TotalCount + ";" + totalPage + page + souType + PageSize);
        Integer total = (int) Math.ceil(TotalCount / PageSize);
        totalPage = total.toString();
        return Constants.QUERY_NO_DATA;
    }

    /**
     * 根据云海的Appkey来获取 云海的磁盘列表
     * @param appkey
     * @return 获取的列表数据为 jsp页面，具体请到 配置文件中 查看 shot.xml
     */
    public String YunHaiSnapShots(Appkey appkey) {
        VolumeClient diskClient = OpenClientFactory.getVolumeClient();
        InstanceClient instanceClient = OpenClientFactory.getInstanceClient();
        ImageClient imageClient = OpenClientFactory.getImageClient();
        if (null == appkey) return Constants.QUERY_NO_DATA;
        SnapshotsDetailReponse snapshotsDetailReponse = getYunHaiShotsBySouType(appkey);
        SnapshotsDetailReponse ShotNetWork = null;
        if(snapshotsDetailReponse==null) {
            return Constants.QUERY_NO_DATA;
        }
        if (snapshotsDetailReponse.getMessage() == null) {
            List<SnapshotDetailItem> shots = snapshotsDetailReponse.getSnapshots();
            if ("datanet".equals(souType)) {
                List<SnapshotDetailItem> shotNetwork = ShotNetWork.getSnapshots();
                shots.addAll(shotNetwork);
            }
            TotalCount = snapshotsDetailReponse.getTotalCount();
            Integer total = (int) Math.ceil(TotalCount / PageSize);  //向上取整
            totalPage = total.toString();
            if (CollectionUtil.isEmpty(shots)) {
                return Constants.QUERY_NO_DATA;
            }
            for (SnapshotDetailItem shot : shots) {
                String diskName = null;
                String diskType = null;
                String shotRate = null;
                String instanceName = null;
                try {
                    DisksDetailReponse DiskList = diskClient.DescribeDisks(regionId, zoneId,
                            "[" + shot.getDiskId() + "]", null, null, null, null, null, null, null, null, page, PageSize.toString(), null,
                            appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                    LOGGER.info(ToStringBuilder.reflectionToString(DiskList));
                    DiskDetailItem diskItem = DiskList.getDisks().get(0);
                    diskName = diskItem.getDiskName();
                    diskType = diskItem.getDiskType();
                    if ("attached".equals(diskItem.getAttachStatus())) {//查询挂载主机
                        DescribeInstancesResponse instanceResponse = instanceClient.DescribeInstances(regionId, zoneId,
                                "[" + diskItem.getInstanceId() + "]", null, null, null, null, null, null, null, null, null,
                                appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                        LOGGER.info(ToStringBuilder.reflectionToString(instanceResponse));
                        if (instanceResponse.getInstances().size() > 0) {
                            InstanceAttributes instanceAttributes = instanceResponse.getInstances().get(0);
                            instanceName = "(" + instanceAttributes.getInstanceName() + ")";
                            LOGGER.info("主机名称：" + instanceName);
                        } else {
                            LOGGER.warn("instanceId:" + diskItem.getInstanceId() + " not found!");
                        }
                    }
                } catch (Exception e) {
                    shot.setDiskId(null);
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String date = simpleDateFormat.format(shot.getCreateTime());
                try {
                shotServers.add(new ShotStatus(regionId, shot.getSnapshotId(), shot.getSnapshotName(),
                        shot.getDiskId(), diskName, instanceName, shot.getSize(), diskType,
                        date, shotRate, shot.getStatus(), Constants.YUN_HAI, appkey.getUserEmail(), appname));
                } catch(Exception e) {
                	LOGGER.info(e.getMessage());
                }
                LOGGER.info("shotList:" + "diskId:" + shot.getDiskId() + "shotId:" + shot.getSnapshotId() + "instanceName" + instanceName);
            }
        } else {
            result = "1";
        }
        return Constants.YUN_HAI;
    }


    /**
     * 云海根据传回的souType来判断是对那些属性进行查询
     * @param appkey
     * @return 返回云海API返回的响应
     */
    public SnapshotsDetailReponse getYunHaiShotsBySouType(Appkey appkey){
        SnapshotClient shotClient = OpenClientFactory.getSnapshotClient();
        SnapshotsDetailReponse snapshotsDetailReponse = null;
        switch (souType) {
            case "all":
                snapshotsDetailReponse = shotClient.DescribeSnapshot(regionId, zoneId, null, null, null, "AVAILABLE", null, null, null, null, page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "system":
                snapshotsDetailReponse = shotClient.DescribeSnapshot(regionId, zoneId, null, null, null, "AVAILABLE", null, null, "SYSTEM", null, page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "datanet":
                //是否显示newwork搜索
                snapshotsDetailReponse = shotClient.DescribeSnapshot(regionId, zoneId, null, null, null, "AVAILABLE", null, null, "DATA", null, page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
//                ShotNetWork = shotClient.DescribeSnapshot(regionId, null, null, null, "AVAILABLE", null, null, "NETWORK", null, page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "keyword":
                snapshotsDetailReponse = shotClient.DescribeSnapshot(regionId, zoneId, null, null, skeyword, "AVAILABLE", null, null, shotDiskType, null, page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
        }
        return snapshotsDetailReponse;
    }

    /**
     * 根据阿里云的Appkey来获取阿里云的快照列表
     * @param appkey
     * @return 获取的列表数据为 jsp页面，具体请到 配置文件中 查看 shot.xml
     */
    public String AliYunSnapShots(Appkey appkey) {
        AliSnapshotClient aliSnapshotClient = OpenClientFactory.getAliSnapshotClient();
        DescribeSnapshotsResponse snapshotsResponse;
        if (StringUtil.isNotEmpty(aliYunSearchType) && StringUtil.isNotEmpty(aliYunKeyWord)) {
            snapshotsResponse = getAliYunSnapResponseBySearchType(appkey);
        } else if (StringUtil.isNotEmpty(aliSourceDiskType)) {
            snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, null, null, null, null, null, null, aliSourceDiskType, null,
                    page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
        } else {
            snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, null, null, null, null, null, null, null, null,
                    page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
        }
        if (snapshotsResponse == null) {
            LOGGER.info("获取阿里云 regionId = " + regionId + "的快照列表失败");
            return Constants.QUERY_NO_DATA;
        }
        TotalCount = snapshotsResponse.getTotalCount();
        Integer total = (int) Math.ceil(TotalCount / PageSize);  //向上取整
        totalPage = total.toString();
        List<DescribeSnapshotsResponse.Snapshot> snapshots = snapshotsResponse.getSnapshots();
        if (CollectionUtil.isNotEmpty(snapshots)) {
            LOGGER.info("获取阿里云 regionId = " + regionId + "的快照列表成功 大小为 " + snapshots.size());
            for (DescribeSnapshotsResponse.Snapshot snapshot : snapshots) {
                String diskSize = snapshot.getSourceDiskSize();
                if (StringUtil.isNotEmpty(diskSize)) {
                    ShotStatus shotStatus = new ShotStatus(regionId, snapshot.getSnapshotId(), snapshot.getSnapshotName(), snapshot.getSourceDiskId(),
                            snapshot.getSourceDiskId(), null, Integer.parseInt(diskSize), snapshot.getSourceDiskType().getStringValue(),
                            snapshot.getCreationTime(), null, snapshot.getStatus(), provider, appkey.getUserEmail(),appkey.getAppname());
                    shotServers.add(shotStatus);
                }
            }
        } else {
            LOGGER.info("获取阿里云 regionId = " + regionId + "的快照列表成功 大小为 " + snapshots.size());
            return Constants.QUERY_NO_DATA;
        }
        return Constants.ALI_YUN;
    }


    /**
     * 根据传回的搜索参数，来确定阿里云精确查找的类型
     * @param appkey
     * @return 阿里云API返回的响应
     */
    public DescribeSnapshotsResponse getAliYunSnapResponseBySearchType(Appkey appkey) {
        AliSnapshotClient aliSnapshotClient = OpenClientFactory.getAliSnapshotClient();
        DescribeSnapshotsResponse snapshotsResponse = null;
        switch (aliYunSearchType) {
            case "shot-name":
                snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, null, null, aliYunKeyWord, null, null, null, aliSourceDiskType, null,
                        page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "shot-id":
                snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, null, aliYunKeyWord, null, null, null, null, null, null,
                        page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "instance-id":
                snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, null, null, null, null, null, aliYunKeyWord, null, null,
                        page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
            case "disk-id":
                snapshotsResponse = aliSnapshotClient.DescribeSnapshot(regionId, aliYunKeyWord, null, null, null, null, null, null, null,
                        page, PageSize.toString(), appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
                break;
        }
        return snapshotsResponse;
    }

    public String addAmazonShot(Appkey appkey, List<ShotStatus> servers) {
        return null;
    }


    public List<ShotStatus> getShotServers() {
        return shotServers;
    }

    public void setShotServers(List<ShotStatus> shotServers) {
        this.shotServers = shotServers;
    }

    public HashMap<Integer, String> getZoneIdNameMap() {
        return zoneIdNameMap;
    }

    public double getTotalCount() {
        return TotalCount;
    }

    public void setTotalCount(double totalCount) {
        TotalCount = totalCount;
    }

    public String getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(String totalPage) {
        this.totalPage = totalPage;
    }

    public Integer getPageSize() {
        return PageSize;
    }

    public void setPageSize(Integer pageSize) {
        PageSize = pageSize;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getSouType() {
        return souType;
    }

    public void setSouType(String souType) {
        this.souType = souType;
    }

    public  String getSkeyword() {
        return skeyword;
    }

    public  void setSkeyword(String skeyword) {
        this.skeyword = skeyword;
    }

    public static String getShotDiskType() {
        return shotDiskType;
    }

    public static void setShotDiskType(String shotDiskType) {
        ShotListAction.shotDiskType = shotDiskType;
    }

    public String getAliSourceDiskType() {
        return aliSourceDiskType;
    }

    public void setAliSourceDiskType(String aliSourceDiskType) {
        this.aliSourceDiskType = aliSourceDiskType;
    }

    public String getAliYunSearchType() {
        return aliYunSearchType;
    }

    public void setAliYunSearchType(String aliYunSearchType) {
        this.aliYunSearchType = aliYunSearchType;
    }

    public String getAliYunKeyWord() {
        return aliYunKeyWord;
    }

    public void setAliYunKeyWord(String aliYunKeyWord) {
        this.aliYunKeyWord = aliYunKeyWord;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getAppname() {
		return appname;
	}

	public void setAppname(String appname) {
		this.appname = appname;
	}

    @Override
    public String toString() {
        return "ShotListAction{" +
                "regionId='" + regionId + '\'' +
                ", appname='" + appname + '\'' +
                ", TotalCount=" + TotalCount +
                ", totalPage='" + totalPage + '\'' +
                ", PageSize=" + PageSize +
                ", page='" + page + '\'' +
                ", souType='" + souType + '\'' +
                ", result='" + result + '\'' +
                ", aliSourceDiskType='" + aliSourceDiskType + '\'' +
                ", aliYunSearchType='" + aliYunSearchType + '\'' +
                ", aliYunKeyWord='" + aliYunKeyWord + '\'' +
                '}';
    }
}
