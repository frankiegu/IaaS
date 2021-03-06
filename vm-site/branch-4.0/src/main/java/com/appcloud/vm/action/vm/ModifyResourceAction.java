package com.appcloud.vm.action.vm;

import appcloud.openapi.client.InstanceClient;
import appcloud.openapi.response.BaseResponse;
import com.appcloud.vm.action.BaseAction;
import com.appcloud.vm.common.Log;
import com.appcloud.vm.fe.common.Constants;
import com.appcloud.vm.fe.manager.AppkeyManager;
import com.appcloud.vm.fe.model.Appkey;
import com.appcloud.vm.fe.util.OpenClientFactory;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ModifyResourceAction extends BaseAction {

    /**
     * Create By rain
     * 修改主机的资源配置和主机的带宽
     */
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(ModifyResourceAction.class);
    private AppkeyManager appkeyManager = new AppkeyManager();
    private InstanceClient instanceClient = OpenClientFactory.getInstanceClient();
    private Integer userId = this.getSessionUserID();
    private Appkey appkey;

    private String regionId;//地域
    private String zoneId;
    private String instanceId;//主机的id
    private String appname;//所属的appname
    private String modifyType;//修改类型，是修改规格还是带宽
    private String modifyCpuNum;//修改后cpu数目
    private String modifyRamSize;//修改后的内存
    private String modifyBandSize;//修改后的带宽
    private String userEmail;
    private String result = "success";

    public String execute() {
        appkey = appkeyManager.getAppkeyByUserIdAndAppName(userId, appname);
        switch (appkey.getProvider()) {
            case Constants.YUN_HAI:
                modifyResourceYunhai(appkey);
                break;
            case Constants.ALI_YUN:
                modifyResourceAliyun(appkey);
            case Constants.AMAZON:
                modifyResourceAmozon();
            default:
                break;
        }
        return SUCCESS;
    }

    public void modifyResourceYunhai(Appkey appkey) {
        if (null != appkey) {
            BaseResponse modifyResourceResponse = null;
            if ("cpumoy".equals(modifyType)) {
                modifyResourceResponse = instanceClient.ModifyInstanceResource(regionId.trim(), zoneId, instanceId.trim(), modifyCpuNum, modifyRamSize, null, appkey.getAppkeyId(), appkey.getAppkeySecret(), userEmail);
                result = "success";
                String operation = "change " + modifyType+":" +modifyCpuNum+"核;"+modifyRamSize+"G";
                chargeLogInfo(operation, "success", "info");
            } else {
                modifyResourceResponse = instanceClient.ModifyInstanceResource(regionId.trim(), zoneId, instanceId.trim(), null, null, modifyBandSize, appkey.getAppkeyId(), appkey.getAppkeySecret(), userEmail);
                result = "success";
                String operation = "change " + modifyType+":" +modifyBandSize;
                chargeLogInfo(operation, "success", "info");
            }
            if (null != modifyResourceResponse.getErrorCode()) {
                result = modifyResourceResponse.getMessage();
            }
        } else {
            result = "no appkey";
        }
    }

    //将续费的各种日志传到乐志
    public void chargeLogInfo (String operation, String message, String logType) {
        Map<String, String> mapLog = new HashMap<>();
        mapLog.put("userId", userId.toString());
        mapLog.put("device", "vm");
        mapLog.put("deviceId", instanceId);
        mapLog.put("provider", appkey.getProvider());
        mapLog.put("operateType", operation);
        mapLog.put("result", message);
        if (logType.equals("info")) {
            Log.INFO(mapLog, appkey, regionId,zoneId);
        } else if (logType.equals("error")) {
            Log.ERROR(mapLog, appkey, regionId,zoneId);
        }
    }

    public void modifyResourceAliyun(Appkey appkey) {}

    public void modifyResourceAmozon() {}


    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getModifyType() {
        return modifyType;
    }

    public void setModifyType(String modifyType) {
        this.modifyType = modifyType;
    }

    public String getModifyCpuNum() {
        return modifyCpuNum;
    }

    public void setModifyCpuNum(String modifyCpuNum) {
        this.modifyCpuNum = modifyCpuNum;
    }

    public String getModifyRamSize() {
        return modifyRamSize;
    }

    public void setModifyRamSize(String modifyRamSize) {
        this.modifyRamSize = modifyRamSize;
    }

    public String getModifyBandSize() {
        return modifyBandSize;
    }

    public void setModifyBandSize(String modifyBandSize) {
        this.modifyBandSize = modifyBandSize;
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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
