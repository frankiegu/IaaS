import appcloud.distributed.CloudControllerClientWapper;
import appcloud.distributed.configmanager.VersionInfo;
import appcloud.distributed.process.operate.AccountOperate;
import appcloud.distributed.process.operate.AccountOperateImpl;
import appcloud.distributed.process.operate.TransPortOperate;
import appcloud.distributed.process.operate.TransPortOperateImpl;
import appcloud.netty.remoting.common.RequestCode;
import appcloud.netty.remoting.common.ResponseCode;
import appcloud.netty.remoting.protocol.RemoteCommand;
import appcloud.netty.remoting.remote.NettyRemotingClient;
import appcloud.openapi.datatype.AppkeyItem;
import com.distributed.common.constant.EnumConstants;
import com.distributed.common.entity.InstanceBackInfo;
import com.distributed.common.entity.VmBack;
import com.distributed.common.entity.VmHost;
import com.distributed.common.entity.VmImageBack;
import com.distributed.common.factory.DbFactory;
import com.distributed.common.service.db.VmBackupService;
import com.distributed.common.service.db.VmHostService;
import com.distributed.common.utils.ModelUtil;
import com.distributed.common.utils.UuidUtil;
import org.junit.Test;

import java.util.List;

/**
 * Created by Idan on 2018/3/17.
 */
public class CloudTest {

    private String addr = "127.0.0.1:8118";
    private String requestId = UuidUtil.getRandomUuid();
    private VmHostService vmHostService = DbFactory.getVmHostService();
    private TransPortOperate transPortOperate = new TransPortOperateImpl();
    private VmBackupService vmBackupService = DbFactory.getVmBackUpService();
    private AccountOperate accountOperate = AccountOperateImpl.getInstance();

//    @Test
//    public void testSendCheck() {
//        CloudControllerClientWapper cloudControllerClientWapper = new CloudControllerClientWapper(new NettyRemotingClient());
//        cloudControllerClientWapper.start();
//
//        String instanceid = "0f3c5d5383e64c88aa887373ebf96798";
//        InstanceBackInfo instanceBackInfo = new InstanceBackInfo(instanceid, 10036, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
//        RemoteCommand remoteCommand = cloudControllerClientWapper.sendCheckDestRequest(requestId, addr, instanceBackInfo);
//        System.out.println(remoteCommand.toString());
//    }
//
//    @Test
//    public void testVmHost() {
//        List<VmHost> vmHosts = vmHostService.findByParams(null, EnumConstants.HostType.COMPUTE_NODE.toString(), null, null);
//        System.out.println(vmHosts);
//    }

    /**
     * 测试传输文件
     */
    @Test
    public void testTransportImg() {
        String sourceFilePath = "D:\\shit.txt";
        String uuid = "11ada76af24345eda9f30ccc9877d6ab";
        String instanceUuid = UuidUtil.getRandomUuid();

        CloudControllerClientWapper cloudControllerClientWapper = new CloudControllerClientWapper(new NettyRemotingClient());
        cloudControllerClientWapper.start();

        VmBack destVmBack = new VmBack(uuid, instanceUuid, "sourceDataCenter", "destDataSource", "sourceHostUuid", "destHostUuid", "127.0.0.1", "SOURCE","ALIVE");
        transPortOperate.transportImageBack(sourceFilePath, addr, false, destVmBack, cloudControllerClientWapper);
    }

    @Test
    public void testDispatcherAccount() {
//        AppkeyItem tempItem = new AppkeyItem(16, 10349, "yumike18@163.com", "e2bee8e97fb84aa98414cc6462cedd7d","DJ8zzA1TDZjKJfSgltcGeWrTSneTr2pq", "yunhai", "云海");
//        VersionInfo versionInfo = accountOperate.addUserVersion(requestId, tempItem);
//        if (ModelUtil.isNotEmpty(versionInfo)) {
//            // 开始分发
//            System.out.println(versionInfo.toString());
////            Boolean rs = accountOperate.dispatchAccount(requestId, versionInfo);
//        }
    }

}
