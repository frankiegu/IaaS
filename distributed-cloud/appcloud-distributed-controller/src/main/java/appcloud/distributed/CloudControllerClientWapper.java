package appcloud.distributed;

import appcloud.distributed.common.Constants;
import appcloud.distributed.configmanager.RouteInfo;
import appcloud.distributed.configmanager.RouteInfoManager;
import appcloud.distributed.configmanager.VersionInfo;
import appcloud.distributed.configmanager.VersionInfoManager;
import appcloud.distributed.header.*;
import appcloud.distributed.util.NumUtil;
import appcloud.distributed.util.VersionUtil;
import appcloud.netty.remoting.RemoteClient;
import appcloud.netty.remoting.common.RequestCode;
import appcloud.netty.remoting.common.ResponseCode;
import appcloud.netty.remoting.common.SerializeType;
import appcloud.netty.remoting.exception.RemotingConnectionException;
import appcloud.netty.remoting.exception.RemotingSendRequestException;
import appcloud.netty.remoting.exception.RemotingTimeOutException;
import appcloud.netty.remoting.protocol.RemoteCommand;
import appcloud.netty.remoting.remote.NettyRemotingClient;
import com.distributed.common.entity.InstanceBackInfo;
import com.distributed.common.entity.VmBack;
import com.distributed.common.utils.ModelUtil;
import com.distributed.common.utils.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lizhenhao on 2017/12/6.
 */
public class CloudControllerClientWapper {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CloudControllerClientWapper.class);

    public RemoteClient remoteClient;
    private RouteInfoManager routeInfoManager = RouteInfoManager.getRouteInfoManager();
    private final VersionInfoManager versionInfoManager = VersionInfoManager.getInstance();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    public CloudControllerClientWapper(RemoteClient remoteClient) {
        this.remoteClient = remoteClient;
    }

    public static volatile CloudControllerClientWapper clientWapper = null;

    public static CloudControllerClientWapper getInstance() {
        if (clientWapper == null) {
            synchronized (CloudControllerClientWapper.class) {
                if (clientWapper == null) {
                    clientWapper = new CloudControllerClientWapper(new NettyRemotingClient());
//                    clientWapper.start();
                }
            }
        }
        return clientWapper;
    }

    public void start() {
        remoteClient.start();
    }

    public void shutdown() {
        remoteClient.shutdown();
    }


    /**
     * 发送注册请求
     *
     * @param ownInfo
     * @param addr
     */
    public void sendRegisterRequest(RouteInfo ownInfo, String addr) {
        RegisterConsumerHeader registerConsumerHeader = new RegisterConsumerHeader(ownInfo);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.REGISTER_BROKER, SerializeType.JSON, registerConsumerHeader);
        invokeOneWayWapper(addr, request, Constants.ONEWAY_SEND_TIME_WAIT);

    }

    /**
     * 分发注册表请求
     *
     * @param list
     * @param masterInfo
     */
    public void sendDispatchRegisterRequest(List<RouteInfo> list, RouteInfo masterInfo, String addr) {
        DispatchRegisterHeader dispatchRegisterHeader = new DispatchRegisterHeader(list, masterInfo);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.DISPATCH_REGISTER_MESSAGE, SerializeType.JSON, dispatchRegisterHeader);
        invokeOneWayWapper(addr, request, Constants.ONEWAY_SEND_TIME_WAIT);

    }

    /**
     * 发送心跳请求
     *
     * @param addr
     * @return
     */
    public RemoteCommand sendHeartBeatMonitorRequest(String addr) {
        HeartBeatHeader heartBeatHeader = new HeartBeatHeader(System.currentTimeMillis());
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.HEART_BEAT, SerializeType.JSON, heartBeatHeader);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
    }

    /**
     * 发送宕机请求
     *
     * @param downBrokerInfo
     * @param newMasterInfo
     * @param masterAlive
     * @param addr
     * @return
     */
    public RemoteCommand sendBrokerDownRequest(RouteInfo downBrokerInfo, RouteInfo newMasterInfo, boolean masterAlive, String addr) {
        BrokerDownHeader brokerDownHeader = new BrokerDownHeader(downBrokerInfo, newMasterInfo, masterAlive);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.BROKER_DOWN, SerializeType.JSON, brokerDownHeader);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
    }

    /**
     * 检查目的数据中心的数据
     * @param addr
     */
    public RemoteCommand sendCheckDestRequest(String requestId, String addr, InstanceBackInfo instanceBackInfo) {
        DefaultHeader<InstanceBackInfo> checkHeader = new DefaultHeader<>(requestId, instanceBackInfo);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.CHECK_BACKUP_DEST, SerializeType.JSON, checkHeader);
        LOGGER.info("request:"+request);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
    }


    /**
     * 将准备传输的请求发送到增量镜像所在的云平台
     *
     * @param addr
     * @param needToClean
     */
    public RemoteCommand sendReadyToTransFileRequest(String requestId, String addr, String instanceUuid, String diskId, boolean needToClean, String appkeyId, String appkeySecret, String userEmail,String accountName,String destDataCenter) {
        ReadyToTransHeader readyToTransHeader = new ReadyToTransHeader(requestId, instanceUuid, diskId, needToClean, appkeyId, appkeySecret,userEmail, accountName, destDataCenter);
        LOGGER.info("传输请求开始，readyToTransHeader: "+readyToTransHeader.toString());
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.READY_IMAGE_BACK, SerializeType.JSON, readyToTransHeader);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT_LONG);
    }

    /**
     * 发送快照传输请求
     *
     * @param bytes
     * @param addr
     * @param transportEnd
     * @param filePath
     * @param position
     * @return
     */
    public RemoteCommand sendImageBackRequest(byte[] bytes, String addr, boolean transportEnd, String filePath, long position, VmBack vmBack) {
        TransportFileHeader transportFileHeader = new TransportFileHeader(vmBack, filePath, transportEnd, position);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.SEND_IMAGE_BACK, SerializeType.JSON, transportFileHeader);
        request.setBody(bytes);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
    }

    /**
     * 发送持续连接请求
     *
     * @param addr
     * @return
     */
    public RemoteCommand sendChkConnectionRequest(String addr) {
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.CHECK_CONNECTION, SerializeType.JSON, null);
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
    }

    /**
     * 添加用户
     *
     * @param newUserEmail
     * @param groupSecretKey
     * @param appkeyId
     * @param appkeySecret
     * @return
     */
    public RemoteCommand sendCreateUserRequest(String requestId, String regionId, String zoneId, String newUserEmail, String groupSecretKey, String appkeyId, String appkeySecret,String accountName) {
        AccountHeader accountHeader = new AccountHeader(requestId, regionId, zoneId, newUserEmail, groupSecretKey, appkeyId, appkeySecret,accountName);
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.USER_CREATE, SerializeType.JSON, accountHeader);
        RouteInfo masterInfo = routeInfoManager.getMasterRouteInfo();
        LOGGER.info("masterInfo: "+masterInfo.toString()+"\r\n"
                +"header:"+accountHeader.toString());
        if (masterInfo == null) {
            LOGGER.error("master is null");
        }
        String addr = masterInfo.getAddr();
        return invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT_LONG);
    }

    /**
     * 分发版本消息，TODO 是否需要异步
     *
     * @param info
     * @return
     */
    public void dispatchVersion(String requestId, VersionInfo info) {
        List<RouteInfo> routeInfoList = routeInfoManager.getAllRouteInfo();
        VersionInfoHeader versionHeader = new VersionInfoHeader(requestId, info);
        LOGGER.info("总共数据中心："+routeInfoList.size());
        LOGGER.info("分发的header："+versionHeader.toString());
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.VERSION_DISPATCH, SerializeType.JSON, versionHeader);
        LOGGER.info("发送的requet:"+request.toString());
        for (RouteInfo routeInfo : routeInfoList) {
            String addr = routeInfo.getAddr();
            if (!addr.equals(routeInfoManager.getMasterAddr())) {
                LOGGER.info("开始向 "+addr+" 发送数据");
                invokeOneWayWapper(addr, request, Constants.ONEWAY_SEND_TIME_WAIT);
            }
        }
    }

    /**
     * 向addr发送当前的版本信息
     * @param version
     * @param addr
     * @return
     */
    public void sendVersionRequest(String requestId, Long version, String addr) {
        VersionHeader versionHeader = new VersionHeader(requestId, version);
        LOGGER.info("请求版本是否相同，header"+versionHeader.toString());
        RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.VERSION_SYN, SerializeType.JSON, versionHeader);
        RemoteCommand response = invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
        if (!response.getRemark().equals(ResponseCode.SUCCESSED)){
            LOGGER.warn("账户同步信息出错");
            return;
        }
        VersionInfoListHeader versionInfoListHeader = (VersionInfoListHeader) response.decodeConsumerHeader(VersionInfoListHeader.class);
        if (ModelUtil.isEmpty(versionInfoListHeader) || versionInfoListHeader.getInfoList() == null) {
            LOGGER.warn("账户同步信息，返回version为null");
        } else {
            List<VersionInfo> infoList = versionInfoListHeader.getInfoList();
            versionInfoManager.synInfo(infoList);
        }
    }

    /**
     * 新的master需要收集集群中所有日志版本最新版本号，然后最新版本去同步
     * @return
     */
    public void refreshVersionRequest() {
        LOGGER.info("开始更新版本……");
        String requestId = UuidUtil.getRandomUuid();
        List<RouteInfo> routeInfoList = routeInfoManager.getAllRouteInfo();
        List<Callable<RemoteCommand>> callables = new ArrayList<>();
        for (RouteInfo routeInfo: routeInfoList) {
            String addr = routeInfo.getAddr();
            callables.add(versionCallable(requestId, addr));
        }
        /**
         * //TODO 这个地方的地址引用是否有问题
         */
        Long latestVersionNum = versionInfoManager.getLatestVersionNum();
        Long selfVersionNum = latestVersionNum;
        String selfAddr = routeInfoManager.getMasterAddr();
        String latestAddr = selfAddr;
        LOGGER.info("新的master需要更新自己本地的版本，本地版本是："+latestVersionNum+", 本地地址是addr："+selfAddr);
        final Map<Long, String> responseVersionMap = new HashMap<>();
        try {
            executorService.invokeAll(callables)
                    .stream()
                    .map(future -> {
                       try {
                           return future.get();
                       } catch (Exception e) {
                           e.printStackTrace();
                           return null;
                       }
                    })
                    .forEach(remoteCommand -> {
                        if (ModelUtil.isNotEmpty(remoteCommand)) {
                            LOGGER.info("版本更新返回的结果："+remoteCommand);
                            VersionRefreshHeader header = (VersionRefreshHeader) remoteCommand.decodeConsumerHeader(VersionRefreshHeader.class);
                            if (ModelUtil.isNotEmpty(header) && NumUtil.isNotEmpty(header.getVersionNum())) {
                                responseVersionMap.put(header.getVersionNum(), header.getAddr());
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 遍历找到版本最大的，然后发送同步该版本的请求
        for (Map.Entry<Long, String> mapEntry: responseVersionMap.entrySet()) {
            if (VersionUtil.compare(latestVersionNum, mapEntry.getKey())>0) {
                latestVersionNum = mapEntry.getKey();
                latestAddr = mapEntry.getValue();
            }
        }
        LOGGER.info("当前最新版本："+latestVersionNum+", 地址："+latestAddr);
        if (latestAddr.equals(selfAddr)) {
            LOGGER.info("当前master的版本就是最新的");
        } else {
            LOGGER.info("当前master的版本不是最新的，需要向最新版本的地址发送同步请求");
            sendVersionRequest(requestId, selfVersionNum, latestAddr);
        }
    }

    public Callable<RemoteCommand> versionCallable(String requestId, String addr) {
        Callable<RemoteCommand> callable = () -> {
            DefaultHeader<String> header = new DefaultHeader<>(requestId, "collect latest version");
            RemoteCommand request = RemoteCommand.createRequestRemoteCommand(RequestCode.VERSION_REFRESH, SerializeType.JSON, header);
            RemoteCommand response = invokeSyncWapper(addr, request, Constants.SYNC_TIME_WAIT);
            return response;
        };
        return callable;
    }

    private RemoteCommand invokeSyncWapper(String addr, RemoteCommand remoteCommand, long timeoutmillions) {
        try {
            return remoteClient.invokeSync(addr, remoteCommand, timeoutmillions);
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException {} , address is {}", e, addr);
            return RemoteCommand.createReponseRemoteCommand(remoteCommand.getCode(), ResponseCode.ERROR);
        } catch (RemotingConnectionException e) {
            LOGGER.error("RemotingConnectionException {} , address is {}", e, addr);
            return RemoteCommand.createReponseRemoteCommand(remoteCommand.getCode(), ResponseCode.ERROR);
        } catch (RemotingTimeOutException e) {
            LOGGER.error("RemotingTimeOutException {} , address is {}", e, addr);
            return RemoteCommand.createReponseRemoteCommand(remoteCommand.getCode(), ResponseCode.ERROR);
        } catch (RemotingSendRequestException e) {
            LOGGER.error("RemotingSendRequestException {} , address is {}", e, addr);
            return RemoteCommand.createReponseRemoteCommand(remoteCommand.getCode(), ResponseCode.ERROR);
        }catch (Exception e) {
            return RemoteCommand.createReponseRemoteCommand(remoteCommand.getCode(), ResponseCode.ERROR);
        }
    }

    private void invokeOneWayWapper(String addr, RemoteCommand remoteCommand, long timeoutmillions) {
        try {
            this.remoteClient.invokeOneWay(addr, remoteCommand, Constants.ONEWAY_SEND_TIME_WAIT);
        } catch (RemotingTimeOutException e) {
            LOGGER.error("RemotingTimeOutException {} , address is {}", e, addr);
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException {} , address is {}", e, addr);
        } catch (RemotingSendRequestException e) {
            LOGGER.error("RemotingSendRequestException {} , address is {}", e, addr);
        } catch (RemotingConnectionException e) {
            LOGGER.error("RemotingConnectionException {} , address is {}", e, addr);
        }
    }

}
