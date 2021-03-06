<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, appcloud.admin.common.Constants"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<html>
<head>
<base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() %>/"/>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>日志查询 - 云海IaaS</title>
<s:include value="/template/_head.jsp" />
<link rel="stylesheet" media="all" type="text/css" href="<%=Constants.FRONT_URL%>css/jquery-ui.css" />
<link rel="stylesheet" media="all" type="text/css" href="<%=Constants.FRONT_URL%>css/jquery-ui-timepicker-addon.css" />
</head>
<body>
<div id="container">
<s:include value="/template/_banner.jsp?menu=runtime" />
<div id="inner">
	<s:include value="/runtime/_left.jsp?menu=log" />
    <div class="right"> 
		<div class="divline bottommargin_20">日志查询</div>
		<div class="middleveralign contentline">
			<span>起始时间<input type="text" name="rest_example_4_start" id="rest_example_4_start" value="" class="squareinput leftmargin_10 rightmargin_20" style="width:175px"/></span>
            <span class="leftmargin_10">终止时间<input type="text" name="rest_example_4_end" id="rest_example_4_end" value="" class="squareinput leftmargin_10 rightmargin_20" style="width:175px" /></span>
		</div>
		<div class="form-group">
			<label class="col-md-2 control-label">查找顺序</label>
			<div class="col-md-10">
				<label class="radio-inline">
					<input type="radio" name="timeasc"  value="yes"/>顺序
				</label>
				<label class="radio-inline">
					<input type="radio" name="timeasc" checked="true" value="no">倒序
				</label>
			</div>
		</div>
			<div class="topmargin_10 bottommargin_10">
			功能实体<select class="selectbox leftmargin_10 rightmargin_20" style="height:24px;width:200px;" id="module">
	        <option value="">--全部--</option>
	        <option value="RESOURCE_SCHEDULER">RESOURCE_SCHEDULER</option>
	        <option value="VOLUME_SCHEDULER">VOLUME_SCHEDULER</option>
	        <option value="VOLUME_PROVIDER">VOLUME_PROVIDER</option>
	        <option value="VM_SCHDULER">VM_SCHDULER</option>
	        <option value="VM_CONTROLLER">VM_CONTROLLER</option>
	        <option value="IMAGE_SERVER">IMAGE_SERVER</option>
	        <option value="NODE_AGENT">NODE_AGENT</option>
	        <option value="NODE_MONITOR">NODE_MONITOR</option>
            <option value="NETWORK_PROVIDER">NETWORK_PROVIDER</option>
            <option value="DHCP_CONTROLLER">DHCP_CONTROLLER</option>
            <option value="LOL_SERVER">LOL_SERVER</option>
            <option value="API_SERVER">API_SERVER</option>
            <option value="VM_FRONT">VM_FRONT</option>
            <option value="IAAS_CHECK">IAAS_CHECK</option>
            <!-- 
            <option value="VM_ADMIN">VM_ADMIN</option>
             -->
            <option value="UN_KNOWN">UN_KNOWN</option>
	        </select>
			日志级别<select class="selectboxsmall leftmargin_10 rightmargin_20" id="level">
	        <option value="">--全部--</option>
	        <option value="INFO">INFO</option>
	        <option value="DEBUG">DEBUG</option>
	        <option value="WARN">WARN</option>
	        <option value="ERROR">ERROR</option>
	        </select>
	        <div class="hidden" id="more">
	            <table class="border:0">
	            <tr height="38px"><td colspan="6"><span>用户邮箱</span><input type="text" class="squareinput leftmargin_10 rightmargin_20" style="width:244px;" id="email" onblur="checkEmail()"/>
                <span>虚拟机/硬盘标识</span><input type="text" class="squareinput leftmargin_5 rightmargin_20" style="width:256px;" id="vmuuid" onblur="checkVmuuid()"/>
                <span id="error" class="redletter"></span></td></tr>
	            <tr height="38px"><td width="50px">用户ID</td><td width="100px"><input type="text" class="squareinputlt leftmargin_5 rightmargin_20" id="user" onblur="checkUser()"/>
			    </td><td width="50px">事务ID</td><td width="100px"><input type="text" class="squareinputlt leftmargin_5 rightmargin_20" id="transaction" onblur="checkTransaction()"/>
			    </td><td width="50px">IP地址</td><td><input type="text" class="squareinput leftmargin_5 rightmargin_20" id="ip" onblur="checkIP()"/>
	            <span id="error1" class="redletter"></span></td></tr>
	            <tr height="38px">
	            <td>操作</td><td><input type="text" class="squareinputlt leftmargin_5 rightmargin_20" id="operation" onblur="checkOperation()"/>
			    </td><td>操作源</td><td><input type="text" class="squareinputlt leftmargin_5 rightmargin_20" id="source" onblur="checkSource()"/>
			    </td><td>日志内容</td><td><input type="text" class="squareinput leftmargin_5 rightmargin_20" id="log" onblur="checkLog()"/>
	            <span id="error2" class="redletter"></span></td></tr>
	            </table>
	        </div>
	        <span id="searchbutton">
	        <button class="button" onclick="submitAll()">查询</button>
            <a class="blueletter leftmargin_10" href="javascript:void(0)" onclick="showMore(this)">高级</a>
            <a class="blueletter leftmargin_10" href="javascript:void(0)" onclick="cancelSearch()">取消</a>
            </span>
        </div>
		<div class="dottedline"></div>
		<div id="query"></div>
	</div><!--right-->
</div>
<s:include value="/template/_footer.jsp" />
</div><!--#container-->
<s:include value="/template/_common_js.jsp" />
<script type="text/javascript" src="<%=Constants.FRONT_URL%>js/public.js"></script>
<script type="text/javascript" src="<%=Constants.FRONT_URL%>js/plugin/jquery-ui.min.js"></script>
<script type="text/javascript" src="<%=Constants.FRONT_URL%>js/plugin/jquery-ui-timepicker-addon.js"></script>
<script type="text/javascript" src="<%=Constants.FRONT_URL%>js/plugin/jquery-ui-sliderAccess.js"></script>
<script>
$(document).ready(function(){
	var startDateTextBox = $('#rest_example_4_start');
    var endDateTextBox = $('#rest_example_4_end');
    
    startDateTextBox.datetimepicker({ 
        timeFormat: 'HH:mm:ss',
        stepHour: 1,
        stepMinute: 1,
        stepSecond: 5,
        onClose: function(dateText, inst) {
            if (endDateTextBox.val() != '') {
                var testStartDate = startDateTextBox.datetimepicker('getDate');
                var testEndDate = endDateTextBox.datetimepicker('getDate');
                if (testStartDate > testEndDate)
                    endDateTextBox.datetimepicker('setDate', testStartDate);
            }
            /* else {
                endDateTextBox.val(dateText);
            } */
        },
        onSelect: function (selectedDateTime){
            endDateTextBox.datetimepicker('option', 'minDate', startDateTextBox.datetimepicker('getDate') );
        }
    });
    endDateTextBox.datetimepicker({
    	timeFormat: 'HH:mm:ss',
        stepHour: 1,
        stepMinute: 1,
        stepSecond: 5,
        onClose: function(dateText, inst) {
            if (startDateTextBox.val() != '') {
                var testStartDate = startDateTextBox.datetimepicker('getDate');
                var testEndDate = endDateTextBox.datetimepicker('getDate');
                if (testStartDate > testEndDate)
                    startDateTextBox.datetimepicker('setDate', testEndDate);
            }
            /* else {
                startDateTextBox.val(dateText);
            } */
        },
        onSelect: function (selectedDateTime){
            startDateTextBox.datetimepicker('option', 'maxDate', endDateTextBox.datetimepicker('getDate') );
        }
    });
    $("#rest_example_4_end").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#rest_example_4_start").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#email").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#vmuuid").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#user").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#transaction").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#ip").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#operation").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#source").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("#log").keydown(function (event) {
        if (event.which == "13" || event.keyCode == "13" || 
                window.event.which == "13" || window.event.keyCode == "13") {
            submitAll();//回车键，用.ajax提交表单
        }
    });
    $("input[name='timeasc']").click(function(event) {
        that = $(this);
        $("input[name='timeasc']").prop("checked", false);
        that.prop("checked", true);
    });
});

function showMore(obj){
	$("#more").toggleClass("hidden");
	if($("#more").hasClass("hidden")){
		obj.innerHTML = "高级";
		$("#searchbutton").css("margin-left", "0px");
	}else{
		$("#searchbutton").css("margin-left", "550px");
		obj.innerHTML = "收起";
	}
}

function cancelSearch(){
	$("#query").html("");
	$("#rest_example_4_start").val("");
	$("#rest_example_4_end").val("");
	$("#module").val("");
	$("#level").val("");
	$("#user").val("");
	$("#transaction").val("");
	$("#ip").val("");
	$("#operation").val("");
	$("#source").val("");
	$("#log").val("");
	$("#email").val("");
    $("#vmuuid").val("");
    $("#error").html("");
    $("#error1").html("");
    $("#error2").html("");
}

function submitAll(){
	var starttime = $("#rest_example_4_start").val();
    var endtime = $("#rest_example_4_end").val();
    var module = $("#module").val();
    var level = $("#level").val();
    var userId = $("#user").val();
    var transactionId = $("#transaction").val();
    var ip = $("#ip").val();
    var operation = $("#operation").val();
    var source = $("#source").val();
    var logContent = $("#log").val();
    var vmuuid = $("#vmuuid").val();
    var email = $("#email").val();
    var timeasc = $($("input[name='timeasc']:checked")[0]).val();
    if(starttime == "" && endtime == "" && module == "" && level == "" &&
            userId == "" && transactionId == "" && ip == "" && operation == "" &&
            source == "" && logContent == "" && vmuuid == "" && email == ""){
        if(!confirm("查询全部日志？")){
            return;
        }
    }else{
    	if(email != "" && !isEmail(email)){
            $("#error").html("用户邮箱输入的格式不正确");
        }else{
            $("#error").html("");
        }
        if(vmuuid != "" && !isString(vmuuid)){
            $("#error").html("虚拟机/硬盘标识输入的格式不正确");
        }else{
            $("#error").html("");
        }
        if(userId != "" && !isDigit(userId)){
            $("#error1").html("用户ID输入的格式不正确");
            return;
        }else{
            $("#error1").html("");
        }
        if(transactionId != "" && !isString(transactionId)){
            $("#error1").html("事务ID输入的格式不正确");
            return;
        }else{
            $("#error1").html("");
        }
        if(ip != "" && !isIP(ip)){
            $("#error1").html("IP地址输入的格式不正确");
            return;
        }else{
            $("#error1").html("");
        }
        if(operation != "" && !isLineString(operation)){
            $("#error2").html("操作输入的格式不正确");
            return;
        }else{
            $("#error2").html("");
        }
        if(source != "" && !isDotString(source)){
            $("#error2").html("操作源输入的格式不正确");
            return;
        }else{
            $("#error2").html("");
        }
        
        if(starttime != ""){
        	starttime += " 000";
        }
        if(endtime != ""){
        	endtime += " 000";
        }
    }
    
    //alert(starttime);
    //alert(endtime);
    
	$("#query").html("");
	searchLog(starttime, endtime, module, level, userId, transactionId, ip, 
			operation, source, logContent, email, vmuuid, timeasc);
}

function searchLog(starttime, endtime, module, level, userId, transactionId, ip, 
        operation, source, logContent, email, vmuuid, timeasc){
	showLoading();
    $.ajax({
        type:"post", 
        url:"runtime/searchlog", 
        data:{
        	startTime: starttime, endTime: endtime, module: module,
        	level: level, userId: userId, transactionId: transactionId, ip: ip, 
            operation: operation, source: source, logContent:logContent, email:email,
            vmUuid: vmuuid, timeasc: timeasc
        },
        success:function(data){
        	var acMessageLogList = data.messageLogs;
        	if(data.isEmail == 1){
                if($("#query").html() == ""){
                    $("#query").html("<h3 class='centeralign'>无更多结果</h3>");
                }else{
                    $("#query #searchmore").html("<div class='divpage'>无更多结果</div>");
                }
                fillTipBox("error","用户邮箱输入有误！");
            }else if(acMessageLogList == null || acMessageLogList == "" ||
            		acMessageLogList.length == 0){
            	if($("#query").html() == ""){
            		$("#query").html("<h3 class='centeralign'>无更多结果</h3>");
            	}else{
            		$("#query #searchmore").html("<div class='divpage'>无更多结果</div>");
            	}
            }
            else{
                //var st = (data.count - 1) * 20;
                var timeMaps = JSON.parse(JSON.stringify(data.timeMap));
                var present = "";
                var pageHtml = "";
                var flag = $("#query").html();
                if(flag == ""){
                	 present += "<table class='datatable topmargin_20' id=\"querytable\"><tr>"+
                     //"<td width=\"60px\"></td>"+
                     "<td width=\"70px\" class=\"rightpadding_5\">时间</td>"+
                     "<td width=\"190px\" class=\"rightpadding_5 leftpadding_5\">功能实体</td>"+
                     "<td width=\"60px\" class=\"rightpadding_5 leftpadding_5\">日志级别</td>"+
                     "<td width=\"50px\" class=\"rightpadding_5 leftpadding_5\">用户ID</td>"+
                     "<td width=\"135px\" class=\"rightpadding_5 leftpadding_5\">事务ID</td>"+
                     "<td width=\"120px\" class=\"rightpadding_5 leftpadding_5\">IP地址</td>"+
                     "<td width=\"135px\" class=\"rightpadding_5 leftpadding_5\">虚拟机标识</td>"+
                     "<td width='30px' class=\"rightpadding_5 leftpadding_5\">详情</td>"+
                     //"<td width=\"90px\" class=\"rightpadding_5 leftpadding_5\">操作</td>"+
                     //"<td width=\"100px\" class=\"rightpadding_5 leftpadding_5\">操作源</td>"+
                     //"<td class=\"leftpadding_5\">日志内容</td>"+
                     "</tr>";
                }
                for(var i = 0;i < acMessageLogList.length;i++){
                    present += "<tr>"/* +"<td width=\"60px\">"+(st+i+1)+"</td>" */+"<td class=\"rightpadding_5\">"+
                    timeMaps[acMessageLogList[i].logTime].substring(0, timeMaps[acMessageLogList[i].logTime].length - 3) + "</td><td class=\"rightpadding_5 leftpadding_5\">"+
                    acMessageLogList[i].module + "</td><td class=\"rightpadding_5 leftpadding_5\">";
                    if(acMessageLogList[i].logLevel == "ERROR"){
                    	present += "<span class=\"redletter\">"+acMessageLogList[i].logLevel + "</span></td>";
                    }else{
                    	present += acMessageLogList[i].logLevel + "</td>";
                    }
                    present += "<td class=\"rightpadding_5 leftpadding_5\">"+
                    acMessageLogList[i].userId + "</td><td class=\"rightpadding_5 leftpadding_5\">"+
                    acMessageLogList[i].transactionId + "</td><td class=\"rightpadding_5 leftpadding_5\">"+
                    acMessageLogList[i].ipAddress + "</td><td class=\"rightpadding_5 leftpadding_5\">"+
                    acMessageLogList[i].vmHdUuid + "</td><td class=\"rightpadding_5 leftpadding_5\">"+
                    "<a class=\"blueletter webwidget_vertical_menu\" href=\"javascript:void(0)\">查看</a>"+
				    "<ul class=\"webwidget_vertical_menu_content\">"+
					"<li class=\"padding715\">操作:"+acMessageLogList[i].operateDrpt+
					"</li><li style='white-space:nowrap;' class=\"padding715\">操作源:"+acMessageLogList[i].sourceClass+
					"</li><li class=\"padding715\">内容:"+acMessageLogList[i].logContent+
					"</li></ul></td>"+

                    //acMessageLogList[i].operateDrpt + "</td><td class=\"rightpadding_5 leftpadding_5\"><span title='"+
                    //acMessageLogList[i].sourceClass + "'>" + acMessageLogList[i].sourceClass.substring(0,20)+"..."+
                    //"</span></td><td class=\"leftpadding_5\"><span title='"+acMessageLogList[i].logContent+"'>" +
                    //acMessageLogList[i].logContent.substring(0,50)+"..." + "</span></td>"+
                    "</tr>";
                }
                
                if(flag == ""){
                    present += "</table><div class='divpage' id='searchmore'>";
                }
                
                var newtime = timeMaps[acMessageLogList[i-1].logTime + 1];
                if (timeasc == "yes") {
                    if(newtime < endtime || endtime == ""){
                        pageHtml += "<a class='pagenum' href='javascript:void(0)' "+
                            "onclick=\"searchLog('"+newtime+"','"+endtime+"','"+module+"','"+
                            level+"','"+userId+"','"+transactionId+"','"+ip+"','"+operation+
                            "','"+source+"','"+logContent+"','"+email+"','"+vmuuid+"')\">更多</a>";
                    }else{
                        pageHtml += "无更多结果";
                    }
				} else {
                    if(newtime > starttime || starttime == ""){
                        pageHtml += "<a class='pagenum' href='javascript:void(0)' "+
                            "onclick=\"searchLog('"+starttime+"','"+newtime+"','"+module+"','"+
                            level+"','"+userId+"','"+transactionId+"','"+ip+"','"+operation+
                            "','"+source+"','"+logContent+"','"+email+"','"+vmuuid+"')\">更多</a>";
                    }else{
                        pageHtml += "无更多结果";
                    }
				}

                    
                if(flag == ""){
                	present += pageHtml;
                    present += "</div>";
                    $("#query").html(present);
                }else{
                	$("#querytable").append(present);
                    $("#searchmore").html(pageHtml);
                }
                $("#querytable tr:first").removeClass("tabletitle");
                $(".webwidget_vertical_menu").webwidget_vertical_menu();
                getStyle();
            };
            hideLoading();
        }
    });
};

function checkEmail(){
    var email = $("#email").val();
    if(email != "" && !isEmail(email)){
        $("#error").html("用户邮箱输入的格式不正确");
    }else{
        $("#error").html("");
    }
}

function checkVmuuid(){
    var vmuuid = $("#vmuuid").val();
    if(vmuuid != "" && !isString(vmuuid)){
        $("#error").html("虚拟机/硬盘标识输入的格式不正确");
    }else{
        $("#error").html("");
    }
}

function checkUser(){
	var userId = $("#user").val();
	if(userId != "" && !isDigit(userId)){
		$("#error1").html("用户ID输入的格式不正确");
	}else{
		$("#error1").html("");
	}
}

function checkTransaction(){
	var transactionId = $("#transaction").val();
	if(transactionId != "" && !isString(transactionId)){
		$("#error1").html("事务ID输入的格式不正确");
    }else{
        $("#error1").html("");
    }
}

function checkIP(){
	var ip = $("#ip").val();
	if(ip != "" && !isIP(ip)){
        $("#error1").html("IP地址输入的格式不正确");
    }else{
        $("#error1").html("");
    }
}

function checkOperation(){
	var operation = $("#operation").val();
	if(operation != "" && !isLineString(operation)){
        $("#error2").html("操作输入的格式不正确");
    }else{
        $("#error2").html("");
    }
}

function checkSource(){
	var source = $("#source").val();
	if(source != "" && !isDotString(source)){
        $("#error2").html("操作源输入的格式不正确");
    }else{
        $("#error2").html("");
    }
}

function checkLog(){
	//var logContent = $("#log").val();
}

function isIP(ipAddr){
    var reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])(\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])){3}$/;
    if(ipAddr.match(reg) || isIPv6(ipAddr)){
        return true;
    } else {
        return false;
    }
}

function isIPv6(str) {  
    var idx = str.indexOf("::");  
    // there is no "::" in the ip address  
    if (idx == -1) {  
        var items = str.split(":");  
        if (items.length != 8) {  
            return false;  
        } else {  
            for (i in items) {  
                if (!isHex(items[i])) {  
                    return false;  
                }  
            }  
            return true;  
        }  
    } else {  
        // at least, there are two "::" in the ip address  
        if (idx != str.lastIndexOf("::")) {  
            return false;  
        } else {  
            var items = str.split("::");  
            var items0 = items[0].split(":");  
            var items1 = items[1].split(":");  
            if ((items0.length + items1.length) > 7) {  
                return false;  
            } else {  
                for (i in items0) {  
                    if (!isHex(items0[i])) {  
                        return false;  
                    }  
                }  
                for (i in items1) {  
                    if (!isHex(items1[i])) {  
                        return false;  
                    }  
                }  
                return true;  
            }  
        }  
    }  
}  
  
// check whether every char of the str is a Hex char(0~9,a~f,A~F)  
function isHex(str) {  
    if(str.length == 0 || str.length > 4) {  
        return false;  
    }  
    str = str.toLowerCase();  
    var ch;  
    for(var i=0; i< str.length; i++) {  
        ch = str.charAt(i);  
        if(!(ch >= '0' && ch <= '9') && !(ch >= 'a' && ch <= 'f')) {  
            return false;  
        }  
    }  
    return true;  
}

function isDigit(str){
	var reg = /^[0-9]+$/;
    if(str.match(reg)){
        return true;
    } else {
        return false;
    }
}

function isString(str){
    var reg = /^[A-Za-z0-9]+$/;
    if(str.match(reg)){
        return true;
    } else {
        return false;
    }
}

function isDotString(str){
    var reg = /^[A-Za-z0-9\.]+$/;
    if(str.match(reg)){
        return true;
    } else {
        return false;
    }
}

function isLineString(str){
    var reg = /^[_A-Za-z0-9]+$/;
    if(str.match(reg)){
        return true;
    } else {
        return false;
    }
}

//判断是否是email
function isEmail(str){
    var reg = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/;
    return reg.test(str);
}

</script>
</body>
</html>