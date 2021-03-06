package com.appcloud.vm.action.vm;

import appcloud.openapi.client.CommonClient;
import appcloud.openapi.client.InstanceClient;
import appcloud.openapi.client.VolumeClient;
import appcloud.openapi.datatype.InstanceAttributes;
import appcloud.openapi.datatype.InstanceTypeItem;
import appcloud.openapi.response.BaseResponse;
import appcloud.openapi.response.DescribeInstanceTypesResponse;
import appcloud.openapi.response.DescribeInstancesResponse;
import com.appcloud.vm.action.BaseAction;
import com.appcloud.vm.common.Log;
import com.appcloud.vm.fe.common.Constants;
import com.appcloud.vm.fe.manager.AppkeyManager;
import com.appcloud.vm.fe.model.Appkey;
import com.appcloud.vm.fe.util.OpenClientFactory;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyChargeTypeAction extends BaseAction {
	/**
	 * 修改虚拟机名称、描述
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ModifyChargeTypeAction.class);
	
	private AppkeyManager appkeyManager = new AppkeyManager();
	private InstanceClient instanceClient = OpenClientFactory.getInstanceClient();
	private VolumeClient diskClient = OpenClientFactory.getVolumeClient();
	private CommonClient commonClient = OpenClientFactory.getCommonClient();
	private Integer userId = this.getSessionUserID();
	private Appkey appkey;
	//续费的参数
	private String yunType="";//云主机或者是云硬盘
	private String providerEn="";
	private String appname="";
	private String id = "";//云id
	private String chargeType = "";
	private String chargeLength = ""; 
	private String regionId = "";
	private String zoneId = "";
	private String userEmail = "";
	private String result = "success";

	//获得付费单价，注意续费只有云主机和和带宽
	private Map<String, Integer> vmPrice;
	private List<Integer> vmPrices = new ArrayList<Integer>();//主机四种价格计费
	private List<Integer> hdPrices = new ArrayList<Integer>();//硬盤四种价格计费
	private List<Integer> bandPrices = new ArrayList<Integer>();//带宽四种价格计费
	//private List<Integer> Price = new ArrayList<Integer>();//本主机的四种规格

	public String execute() {
		appkey = appkeyManager.getAppkeyByUserIdAndAppName(userId, appname);
		switch (appkey.getProvider()) {
			case Constants.YUN_HAI:
				YunhaiModifyCharge(appkey);
				break;
			case Constants.ALI_YUN:
				AliyunModifyCharge();
				break;
			case Constants.AMAZON:
				AmazonModifyCharge();
				break;
			default:
				break;
		}

		//根据result插入日志
		Map<String, String> mapLog = new HashMap<>();
		mapLog.put("userId", userId.toString());
		mapLog.put("device", yunType);
		mapLog.put("deviceId", id);
		mapLog.put("provider", appkey.getProvider());
		mapLog.put("operateType", "renew " + chargeLength + " "+chargeType);
		mapLog.put("result", result);

		if (result == "success") {
			Log.INFO(mapLog, appkey, regionId,zoneId);
		} else {
			Log.ERROR(mapLog, appkey, regionId,zoneId);
		}
		return SUCCESS;
	}

	public String vmGetPrice(){
		appkey = appkeyManager.getAppkeyByUserIdAndAppName(userId, appname);
		//用instanceId获得实例列表
		DescribeInstancesResponse instanceList = instanceClient.DescribeInstances(regionId.trim(), null,
				"["+id.trim()+"]", null, null, null, null, null, null, null, null, null,
				appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
		// TODO: 2016/9/27 这里有一个下标越界
		InstanceAttributes instance = instanceList.getInstances().get(0);
		price(appkey,instance.getVcpus(),instance.getMemoryMb(),instance.getLocalGb(),instance.getInternetMaxBandwidth());
		return SUCCESS;
	}

	public void YunhaiModifyCharge(Appkey appkey){
		switch(yunType.trim()){
		case "vm":
			vmModifyCharge(appkey);
			break;
		case "hd":
			HdModifyCharge(appkey);
			break;
		}
	}

	public void AliyunModifyCharge(){}

	public void AmazonModifyCharge(){}

	public void vmModifyCharge(Appkey appkey){
		if (null!=appkey){
			BaseResponse modifyInstanceCharge = instanceClient.RenewInstance(regionId.trim(), zoneId, id,chargeType,chargeLength,appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
			logger.info("vmModiChar:" + ToStringBuilder.reflectionToString(modifyInstanceCharge));
			if(modifyInstanceCharge.getMessage()==null){
				result = "success";
			}else{
				result = modifyInstanceCharge.getMessage();
			}
		}else {
			result="no appkey";
		}
		
	}

	public void HdModifyCharge(Appkey appkey){
		if (null!=appkey){
			BaseResponse modifyDiskCharge = diskClient.RenewDisk(regionId.trim(),zoneId,id,chargeType,chargeLength,appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
			logger.info(ToStringBuilder.reflectionToString(modifyDiskCharge));
			if(modifyDiskCharge.getMessage()==null){
				result = "success";
			}else{
				result = modifyDiskCharge.getMessage();
			}
		}else {
			result="no appkey";
		}
	}

	/**
	 * 由主机的cup和内存得到主机单价，再有带宽获得单价，得到总的四种单价
	 * @param appkey
	 * @param instanceCup
	 * @param memorySize
	 * @param bandWidth
	 */
	public void price(Appkey appkey, Integer instanceCup, Integer memorySize, Integer diskSize, String bandWidth) {
		//获得主机所有价格
		DescribeInstanceTypesResponse typesResponse = commonClient.DescribeInstanceTypes(regionId.trim(), zoneId, appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
		if (null==typesResponse.getMessage()){
			List<InstanceTypeItem> instanceTypeItems = typesResponse.getInstanceTypes().getInstanceTypeItems();
			if (instanceTypeItems.size()>0){
				for (InstanceTypeItem instanceTypeItem:instanceTypeItems){
					if ((instanceCup==instanceTypeItem.getCpuCoreCount())&&(memorySize==instanceTypeItem.getMemorySize())){
						vmPrices.add(instanceTypeItem.getHourprice());
						vmPrices.add(instanceTypeItem.getDayPrice());
						vmPrices.add(instanceTypeItem.getMonthPrice());
						vmPrices.add(instanceTypeItem.getYearPrice());
						break;
					}
				}
			}else{
				result = "no instance, no price";
				logger.warn("instance:为什么获取是空");
			}
		}else{
			result = typesResponse.getMessage();
		}
		//获得硬盤所有价格
		DescribeInstanceTypesResponse diskResponse = commonClient.DescribeDiskTypes(regionId.trim(), zoneId, appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
		if (null==diskResponse.getMessage()){
			List<InstanceTypeItem> diskTypeItems = diskResponse.getInstanceTypes().getInstanceTypeItems();
			if (diskTypeItems.size()>0){
				for (InstanceTypeItem instanceTypeItem:diskTypeItems){
					if (diskSize==instanceTypeItem.getHardDisk()){
						hdPrices.add(instanceTypeItem.getHourprice());
						hdPrices.add(instanceTypeItem.getDayPrice());
						hdPrices.add(instanceTypeItem.getMonthPrice());
						hdPrices.add(instanceTypeItem.getYearPrice());
						break;
					}
				}
			}else{
				result = "no disk, no price";
				logger.warn("disk:为什么获取是空");
			}
		}else{
			result = diskResponse.getMessage();
		}
       //获得带宽所有价格
		 DescribeInstanceTypesResponse bandResponse = commonClient.DescribeInternetTypes(regionId, zoneId, appkey.getAppkeyId(), appkey.getAppkeySecret(), appkey.getUserEmail());
         if (null==bandResponse.getMessage()){
        	 List<InstanceTypeItem> bandTypeItems = bandResponse.getInstanceTypes().getInstanceTypeItems();
        	 if (bandTypeItems.size()>0) {
        		 for (InstanceTypeItem instanceTypeItem:bandTypeItems){
      				if (bandWidth.equals(instanceTypeItem.getBandWidth().toString())){
     					bandPrices.add(instanceTypeItem.getHourprice());
     					bandPrices.add(instanceTypeItem.getDayPrice());
     					bandPrices.add(instanceTypeItem.getMonthPrice());
     					bandPrices.add(instanceTypeItem.getYearPrice());
     					break;
      				}
      			}
			}else {
				result = "no bandWidth, no price";
				logger.warn("band:为什么获取是空");
			}
 		}else{
 			result = bandResponse.getMessage();
 		}
         try {
			 if((vmPrices.size()!=0)&&(hdPrices.size()!=0)&&(bandPrices.size()!=0)){
				 vmPrice = new HashMap<>();
				 Integer hPrice = vmPrices.get(0) + hdPrices.get(0) + bandPrices.get(0);
				 vmPrice.put("hPrice", hPrice);
				 Integer dPrice = vmPrices.get(1) + hdPrices.get(1) + bandPrices.get(1);
				 vmPrice.put("dPrice", dPrice);
				 Integer mPrice = vmPrices.get(2) + hdPrices.get(2) + bandPrices.get(2);
				 vmPrice.put("mPrice", mPrice);
				 Integer yPrice = vmPrices.get(3) + hdPrices.get(3) + bandPrices.get(3);
				 vmPrice.put("yPrice", yPrice);
				 logger.info("四种价格："+hPrice+","+dPrice+","+mPrice+","+yPrice);
			 }

         } catch (Exception e) {
        	 logger.error(e.getMessage(),e);
			 result = e.getMessage();
         }
	}

	public String getProviderEn() {
		return providerEn;
	}

	public void setProviderEn(String providerEn) {
		this.providerEn = providerEn;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getChargeType() {
		return chargeType;
	}

	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
	}

	public String getChargeLength() {
		return chargeLength;
	}

	public void setChargeLength(String chargeLength) {
		this.chargeLength = chargeLength;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getYunType() {
		return yunType;
	}

	public void setYunType(String yunType) {
		this.yunType = yunType;
	}

	public Map<String, Integer> getVmPrice() {
		return vmPrice;
	}

	public void setVmPrice(Map<String, Integer> vmPrice) {
		this.vmPrice = vmPrice;
	}

	public String getAppname() {
		return appname;
	}

	public void setAppname(String appname) {
		this.appname = appname;
	}

	public String getZoneId() {
		return zoneId;
	}

	public void setZoneId(String zoneId) {
		this.zoneId = zoneId;
	}
}
