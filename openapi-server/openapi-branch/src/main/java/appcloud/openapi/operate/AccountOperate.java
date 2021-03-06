package appcloud.openapi.operate;

import appcloud.openapi.response.GroupCreateResponse;
import appcloud.openapi.response.UserCreateForDisResponse;
import appcloud.openapi.response.UserCreateResponse;

import java.util.Map;

/**
 * Created by Idan on 2017/10/22.
 */
public interface AccountOperate {

    public UserCreateResponse UserCreate(Map<String, String> paramsMap, String requestId) throws Exception;

    public UserCreateForDisResponse UserCreateForDis(Map<String, String> paramsMap, String requestId) throws Exception;

    public GroupCreateResponse GroupCreate(Map<String, String> paramsMap, String requestId) throws Exception;
}
