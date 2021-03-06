package com.distributed.dbproxy.process;

import appcloud.netty.remoting.common.RequestCode;
import appcloud.netty.remoting.common.ResponseCode;
import appcloud.netty.remoting.common.SerializeType;
import appcloud.netty.remoting.protocol.RemoteCommand;
import appcloud.netty.remoting.remote.NettyRequestProcessor;
import com.distributed.common.entity.VmBack;
import com.distributed.common.remote.header.DbProxyHeader;
import com.distributed.common.remote.header.DbProxyResponseHeader;
import com.distributed.dbproxy.constant.Constants;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Idan on 2017/12/7.
 */
@Deprecated
public class ProxyProcess implements NettyRequestProcessor, InvocationHandler{

    private final static Logger logger = LoggerFactory.getLogger(ProxyProcess.class);

    @Override
    public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) throws Exception {
        RemoteCommand response = null;
        int requestCode = request.getCode();
        if (requestCode != RequestCode.DB_PROXY_REQUEST) {
            logger.error("发送出错……");
        }
        DbProxyHeader dbProxyHeader = null;
        try {
            dbProxyHeader = (DbProxyHeader)request.decodeConsumerHeader(DbProxyHeader.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("发送出错……");
            response = RemoteCommand.createReponseRemoteCommand(RequestCode.DEFAULT, ResponseCode.DEFAULT);
            return response;
        }
        System.out.println("the request is: "+dbProxyHeader.toString());

        Object result = null;
        try {
            String proxyClass = dbProxyHeader.getInterfaceName();
            String implClass = Constants.proxyHashMap.get(proxyClass);
            if (implClass == null) {
                System.out.println("there is no impl class for: "+proxyClass);
                return null;
            }

            Class<?> service = Class.forName(implClass);
            String methodName = dbProxyHeader.getMethodName();
            Class<?>[] parameterTypes = dbProxyHeader.getParameterTypes();
            Method method = service.getMethod(methodName,parameterTypes);
            Object[] args = dbProxyHeader.getInvokeArgs();
            result = method.invoke(service.newInstance(), args);
            logger.info("the result for reflect is:"+result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("the proxy for db failed: "+e.getMessage());
        }

        response = RemoteCommand.createReponseRemoteCommand(RequestCode.SUCCESS, ResponseCode.SUCCESSED, SerializeType.STREAM);
        response.setConsumHeader(new DbProxyResponseHeader<VmBack>((VmBack) result));
        return response;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(args);
    }
}
