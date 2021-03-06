<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <title>云快照 - 云海IaaS</title>
    <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() %>/"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <s:include value="../template/_head.jsp"/>
</head>
<body class="front-body">

<!--首部标签-->
<s:include value="../template/_banner.jsp?menu=shot"/>

<div class="front-inner">
    <div class="container">

        <!---------------------------------- appname和regionId ------------------------------------->
        <div class="col-md-12 form-group">
            <!-- appname -->
            <div class="col-md-2" style="padding-left: 0;">
                <div class="dropdown">
                    <button id="appnameDropMenu" style="text-align: left;" class="btn btn-default dropdown-toggle form-control front-no-box-shadow"
                            type="button"id="selectAppnameBtn" data-toggle="dropdown">
                        <span id="appnameicon" class='glyphicon selectspan'></span>
                        <label id="appnamemenu" style="width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" ></label>
                        <span class="caret rightmiddle"></span>
                        <span id="appproviderEn" class="hidden"></span>
                    </button>
                    <ul id="selectAppname" class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
                    </ul>
                </div>
            </div>
            <!-- 加载当前提供商的地区信息 -->
            <div class="col-md-2" style="padding-left: 0" id="regions">
                <select class="form-control front-no-box-shadow" id="selectRegion" disabled="disabled" onchange="getSnapShots()"></select>
            </div>
            <div class="col-md-8 text-right" style="padding-right: 0px">
                <a id="search_yun" href="javascript:void(0)" onclick="exsearch()" class="btn btn-default"><span class="glyphicon glyphicon-search"></span> 搜索</a>
            </div>
        </div>
        <div class="clearfix"></div>

        <!----------------------------------- 显示快照搜索的列表 ------------------------------->
        <div id="yun-shot-search" class="hidden panel panel-default front-panel">
            <div class="panel-heading">
                <strong>云快照搜索</strong>
            </div>
            <div class="panel-body">
                <div id="select-content"></div>
            </div>
        </div>
        <!----------------------------------- 显示云快照的列表 ------------------------------->
        <div id="shot-content"></div>

        <!--分页标签-->
        <div style="text-align: center" id="back_divPage"></div>
    </div>

    <!--底部标签-->
    <s:include value="../template/_footer.jsp"/>
</div>

<s:include value="../template/_global.jsp"/>
<script src="js/searchbox.js"></script>
<script src="js/shot/shotlist.js"></script>
<script src="js/check_ali.js"></script>
</body>
</html>