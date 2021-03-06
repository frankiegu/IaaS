package com.distributed.common.rpc;

import appcloud.netty.remoting.common.RequestCode;
import appcloud.netty.remoting.common.ResponseCode;
import appcloud.netty.remoting.common.SerializeType;
import appcloud.netty.remoting.protocol.RemoteCommand;
import appcloud.netty.remoting.remote.NettyRequestProcessor;
import com.distributed.common.remote.header.RPCHeader;
import com.distributed.common.remote.header.RPCResponseHeader;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by zouji on 2018/1/15.
 */
public class RPCProcess implements NettyRequestProcessor, InvocationHandler {

    private final static Logger logger = LoggerFactory.getLogger(RPCProcess.class);

    private Map<Class<?>,Object> instanceMap;

    public RPCProcess(Map<Class<?>,Object> instanceMap) {
        this.instanceMap = instanceMap;
    }


    public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) throws Exception {
        RemoteCommand response = null;
        if (request.getCode() != RequestCode.RPC_REQUEST) {
            logger.error("Invalid request code...");
        }
        RPCHeader rpcHeader = null;
        try {
            rpcHeader = (RPCHeader)request.decodeConsumerHeader(RPCHeader.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Decode RPCHeader.class failed!");
            response = RemoteCommand.createReponseRemoteCommand(RequestCode.DEFAULT, ResponseCode.DEFAULT);
            return response;
        }
        logger.info("The request is:" + rpcHeader.toString());

        Object result = null;
        try {
            String interfaceClass = rpcHeader.getInterfaceName();
            Class<?> service = Class.forName(interfaceClass);
            String methodName = rpcHeader.getMethodName();
            Class<?>[] parameterTypes = rpcHeader.getParameterTypes();
            Method method = service.getMethod(methodName, parameterTypes);
            Object[] args = rpcHeader.getInvokeArgs();
            Object instance = instanceMap.get(service);
            if (instance == null) {
                response = RemoteCommand.createReponseRemoteCommand(RequestCode.SYSTEM_ERROR, ResponseCode.ERROR_RPC);
                return response;
            }
            result = method.invoke(instance, args);
            logger.info("the result for reflect is:"+result);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("the reflect failed: "+e.getMessage());
        }
        response = RemoteCommand.createReponseRemoteCommand(RequestCode.SUCCESS, ResponseCode.SUCCESSED, SerializeType.STREAM);
        response.setConsumHeader(new RPCResponseHeader(result));
        return response;
    }

    public boolean rejectRequest() {
        return false;
    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(args);
    }
}
