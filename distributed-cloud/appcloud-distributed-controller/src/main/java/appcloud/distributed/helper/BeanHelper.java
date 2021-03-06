package appcloud.distributed.helper;

import appcloud.distributed.configmanager.Request;
import appcloud.distributed.configmanager.ServiceHandler;
import com.distributed.common.constant.Constants;
import com.distributed.common.utils.ConfigReader;
import com.distributed.common.utils.ReflectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Idan on 2018/1/17.
 * Class<?> 是接口
 */
public class BeanHelper {

    private static final Logger logger = LoggerFactory.getLogger(VersionHelper.class);
    private static final Map<Class<?>, Object> BEAN_MAP = new HashMap<>();
    private static final Map<Class<?>, ServiceHandler> SERVICE_MAP = new HashMap<>();

    /**
     * 根据接口然后在Factory中找到实例
     * //TODO 必须要完成，requestPath和requestMethod找到实例，然后塞入
     * 还是借助了配置文件
     */
    static {
        try {
            TypeReference<List<OpInvokeUnit>> listType =  new TypeReference<List<OpInvokeUnit>>() {};
            List<OpInvokeUnit> opInvokeUnits =  ConfigReader.readFromJson(Constants.class.getClassLoader().getResource("op_invoke.json"), listType);
            for (OpInvokeUnit unit : opInvokeUnits) {
                String service = unit.getService();
                Class<?> serviceClass = ReflectionUtil.getClass(service);
                String factory = unit.getFactory();
                String methodName = unit.getMethod();
                Class<?> factoryClass = ReflectionUtil.getClass(factory);
                Method[] methods = null;
                if (factoryClass != null) {
                    methods=factoryClass.getDeclaredMethods();
                }
                for (Method temp: methods) {
                    if (temp.getName().equals(methodName)) {
//                        Object instance = temp.invoke(factoryClass);
//                        BEAN_MAP.put(serviceClass, instance);
                        setServiceMap(serviceClass, factoryClass, temp);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setServiceMap(Class<?> serviceClass, Class<?> factoryClass, Method serviceMethod) {
        ServiceHandler serviceHandler = new ServiceHandler(factoryClass, serviceMethod);
        SERVICE_MAP.put(serviceClass, serviceHandler);
    }

    public static Map<Class<?>, Object> getBeanMap() {
        return BEAN_MAP;
    }

    public static Map<Class<?>, ServiceHandler> getServiceMap() {
        return SERVICE_MAP;
    }

}
