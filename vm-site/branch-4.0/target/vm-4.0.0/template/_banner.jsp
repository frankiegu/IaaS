<%@ page import="com.appcloud.vm.common.Constants" %>
<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<nav class="navbar navbar-default navbar-fixed-top front-nav">
    <div class="container">
        <div>
            <!-- 品牌图片大小为150 * 30：宽度不定，高度固定30px -->
            <div class="nav-brand"><a href="#"><img src="images/logo.png" alt="brand" class="img-responsive"/></a></div>
            <!-- -->
        </div>
        <div class="nav-collapse collapse" id="nav-collapse-demo">
           <ul class="nav navbar-nav">
                <li class="${param.menu=='sum'?'front-active':''}">
                    <a href="sum/sum">概览</a>
                </li> <!-- 激活菜单 -->
                <li class="${param.menu=='vm'||param.menu=='newvm'||param.menu=='vmdetail'?'front-active':''}">
                    <a href="vm/vmlist.jsp">云主机</a>
                </li>
                <li class="${param.menu=='hd'||param.menu=='hddetail'||param.menu=='newhd'?'front-active':''}">
                    <a href="hd/hd_list.jsp">云硬盘</a>
                </li>
                <li class="${param.menu=='shot'?'front-active':''}">
                    <a href="shot/shotsList.jsp">云快照</a>
                    </li>
               <li class="${param.menu=='images'?'front-active':''}">
                   <a href="image/imagesList.jsp">云镜像</a></li>
               <%--<li class="${param.menu=='fw'?'front-active':''}">--%>
                   <%--<a href="fw/fwlist.jsp">云安全</a></li>--%>
               <li class="dropdown">
                   <a href="#" class="dropdown-toggle" data-toggle="dropdown">帮助<span class="caret"></span></a>
                   <ul class="dropdown-menu nav-dropdown front-min-width">
                       <li><a href="http://freeshare.free4inno.com/resource?id=15403&amp;tipstatus=success" target="_blank"><span class="glyphicon glyphicon-question-sign"></span>&nbsp;&nbsp;使用帮助</a></li>
                       <li><a href="https://www.wenjuan.com/s/AbUr6b/" target="_blank"><span class="glyphicon glyphicon-exclamation-sign"></span>&nbsp;&nbsp;用户反馈</a></li>
                   </ul>
               </li>
                <!-- 下拉菜单 -->
            </ul>
        </div>
        <div class="nav-right">
            <!-- 搜索图标：可选 -->
            <%--<div class="area area-media"><span class="glyphicon glyphicon-search nav-search-icon"></span></div>--%>
            <!-- -->
            <!-- 产品导航菜单按钮 -->
            <div class="area area-media"><span class="glyphicon glyphicon-th nav-toggle-pro" data-gen="nav-pro" data-toggle="front-popover-bottom" data-target="#nav-pro-demo"></span></div>
            <!-- -->
            <!-- 导航栏用户头像 -->
            <div class="area area-avatar area-media"><img src="<%=Constants.ACCOUNT_URL%>users/users/download?uuid=<%=request.getSession().getAttribute("keyProfileImageUrl")%>" class="img-circle nav-avatar" onerror="javascript:this.src='images/user_male.png'" alt="avatar" data-toggle="front-popover-bottom" data-target="#nav-user-demo"/></div>
            <!-- -->
            <!-- 横向导航栏移动端触发：可选 -->
            <div class="area visible-xs nav-toggle-down" data-toggle="collapse" data-target="#nav-collapse-demo"><span class="glyphicon glyphicon-menu-hamburger" id="front-nav-toggle-down-demo"></span></div>
            <!-- -->
            <!-- 产品导航菜单 -->
            <div id="nav-pro-demo" data-pro="3"></div>
            <!-- 用户菜单 -->
            <div id="nav-user-demo" class="bottom nav-popover nav-popover-media nav-avatar-menu">
                <div class="arrow"></div>
                <ul>
                    <li class="text-center">
                        <a href="<%=Constants.ACCOUNT_URL%>users">
                            <!-- 用户头像 -->
                            <img src="<%=Constants.ACCOUNT_URL%>users/users/download?uuid=<%=request.getSession().getAttribute("keyProfileImageUrl")%>" alt="avatar" class="img-circle img-lg-avatar"  onerror="javascript:this.src='images/user_male.png'" />
                            <!-- end 用户头像 -->
                        </a>
                        <div><%=request.getSession().getAttribute("userName")%></div>
                        <div><small><%=request.getSession().getAttribute("email")%></small></div>
                    </li>
                    <li class="divider"></li> <!-- 分界线 -->
                    <li><a href="<%=Constants.ACCOUNT_URL%>users" target="_blank"><span class="glyphicon glyphicon-cog"></span>&nbsp;&nbsp;账号设置</a></li>
                    <li><a href="javascript:void(0)" onclick="logout()"><span class="glyphicon glyphicon-log-out"></span>&nbsp;&nbsp;退出</a></li>
                </ul>
            </div>
            <!-- end 用户菜单 -->
        </div>
    </div>
</nav>
<input id="baseURL" type="hidden" value="<%=request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()%>"/>
<script>
    function logout(){
        $.ajax({
            url : "account/logout",
            type:"post",
            dataType:"json",
            async:false,
            data:{},
            success:function(data) {
                if(typeof(com) != "undefined"){
                    com.xmpp.close();
                }
                var acc_token = "<%=request.getSession().getAttribute("accToken")%>";
                var accountAddr = "<%=Constants.ACCOUNT_URL%>";
                $.ajax({
                    url:accountAddr+"api/oauth2/revokeoauth2?callback=?",
                    type:"post",
                    dataType:"json",
                    data:{'access_token':acc_token},
                    complete:function(result) {
                        if(typeof(com) != "undefined"){
                            com.xmpp.close();
                        }
                        location.replace($("#baseURL").val()+"/sum/sum");
                    },
                    error:function(data){
                        alert('post data error:' + data);
                        console.log(data);
                    }
                });
            },
            error:function(data){
                alert('logout error:' + data);
            }
        });
    }
</script>