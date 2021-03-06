var sliderBandWidth;
var sliderDay;
var sliderMonth;
var sliderYear;

$(document).ready(function () {
    testWindowSize();

    //不知道为什么会出现两条slider，可能是个bug
    $("#day-slider *:first").remove();
    $("#month-slider *:first").remove();
    $("#year-slider *:first").remove();
    sliderDay = new Slider("#day-length");
    sliderMonth = new Slider("#month-length");
    sliderYear = new Slider("#year-length");
    getAppname();

});

var appdata = [];

function getAppname() {
    var checkOnece = true;
    $.ajax({
        type: "POST",
        url: "common/getAppName",
        success: function (data) {
            appdata = data;
            $('#selectAppname').html('');
            var appName;
            var providerEn;
            for (var i in data) {
                appName = data[i].appName;
                providerEn = data[i].providerEn;
                switch (providerEn) {
                    case "aliyun":
                        $appname = $("<li onclick='changeName("+i+")' role='presentation'><a role='menuitem' href='javascript:void(0)'><span class='glyphicon icon-freeshare-aliyun selectspan'></span>"+
                            data[i].appName + "</a><span class='hidden'>"+data[i].providerEn+"</span></li>");
                        break;
                    case "yunhai":
                        $appname = $("<li onclick='changeName("+i+")' role='presentation'><a role='menuitem' href='javascript:void(0)'><span class='glyphicon glyphicon-cloud selectspan'></span>" +
                            data[i].appName + "</a><span class='hidden'>"+data[i].providerEn+"</span></li>");
                        break;
                }
                $('#selectAppname').append($appname);
            }
            var ulMenuName = $("#yunhai_ulMenuName").val();
            var ulMenuProviderEn = $("#yunhai_ulMenuProviderEn").val();
            if(ulMenuName == ""){
                ulMenuName = appName;
                ulMenuProviderEn = providerEn;
                $("#yunhai_appname").val(ulMenuName) ;
            }
            $.ajax({
                type: "POST",
                url: "account/getAccountZoneId",
                data: {
                    name: ulMenuName
                },
                success: function (data) {
                    $("#selectZone").val(data.zone)
                    $("#selectZone").attr("disabled","disabled").css("background-color","#EEEEEE;");
                },
                error:function () {
                    $.fillTipBox({type: 'danger', icon: 'glyphicon-alert', content: '获取云账户所属可用区失败'});
                }
            });
            clickChangeAppName(ulMenuProviderEn, ulMenuName);
            getInfo();
        }
    })
}

function changeName(i) {
    var provierEn = appdata[i].providerEn;
    var appName = appdata[i].appName;
    var zoneId = appdata[i].zone;
    clickChangeAppName(provierEn, appName);
    switch (provierEn) {
        case 'yunhai':
            window.location.href="vm/newvm/newyunhaivm.jsp?appname="+appName+"&ulMenuName="+appName+"&ulMenuProviderEn="+provierEn+"&zone="+zoneId;
            break;
        case 'aliyun':
            window.location.href="vm/newvm/aliyun/newaliyunvm.jsp?appname="+appName+"&ulMenuName="+appName+"&ulMenuProviderEn="+provierEn+"&zone="+zoneId;
            break;
    }
}

function clickChangeAppName(providerEn, appname) {
    if (providerEn=="yunhai") {
        $('#appnameicon').addClass("glyphicon-cloud");
    } else if (providerEn == "aliyun") {
        $('#appnameicon').addClass("icon-freeshare-aliyun");
    }
    $('#appnamemenu').html(appname);
    $('#appproviderEn').html(providerEn);
}


$("#setting-now").click(function () {
    $('#password-box').removeClass('hidden');
    // $('#password-later-box').addClass('hidden');
    $('#setting-now').addClass("active");
    $("#setting-later").removeClass("active");
    instance.settingLater = false;
    refresh();
});

$("#setting-later").click(function () {
    // $('#password-later-box').removeClass('hidden');
    $('#password-box').addClass('hidden');
    $('#setting-now').removeClass("active");
    $("#setting-later").addClass("active");
    instance.settingLater = true;
    var userId = $("#userId").val();
    console.log(userId);
    var time = getNowFormatDate();
    $("#instancename").val(userId + "--" + time);
    refresh();
});
function getNowFormatDate() {
    var date = new Date();
    var seperator1 = "-";
    var seperator2 = ":";
    var month = date.getMonth() + 1;
    var strDate = date.getDate();
    if (month >= 1 && month <= 9) {
        month = "0" + month;
    }
    if (strDate >= 0 && strDate <= 9) {
        strDate = "0" + strDate;
    }
    var currentdate = date.getFullYear() + seperator1 + month + seperator1 + strDate
        + " " + date.getHours() + seperator2 + date.getMinutes()
        + seperator2 + date.getSeconds();
    return currentdate;
}

function testWindowSize() {
    var width = $(window).width();
    console.log(width);
    if (width >= 1200) {
        var leftGutter = (width - 1170) / 2 + 1170 * 0.75;
        var cardWidth = 1170 * 0.25;
        $('#right-card').attr("style", "position: fixed;left: " + leftGutter + "px;" + "width: " + cardWidth + "px");
    } else if (width >= 970) {
        var leftGutter = (width - 970) / 2 + 970 * 0.75;
        var cardWidth = 970 * 0.25;
        $('#right-card').attr("style", "position: fixed;left: " + leftGutter + "px;" + "width: " + cardWidth + "px");
    } else {
        $('#right-card').attr("style", "");
    }
}

function day() {
    $("#month-slider").addClass("hidden");
    $("#year-slider").addClass("hidden");
    $("#day-slider").removeClass("hidden");
    $("#day").addClass("active");
    $('#month').removeClass("active");
    $('#year').removeClass("active");

    instance.timeTypeDisp = "天";
    instance.timeType = "day";
    sliderDay.setValue(0);
    instance.timeNum = Number(sliderDay.getValue()) + 1;
    refresh();
}

function year() {
    instance.timeTypeDisp = "年";
    instance.timeType = "year"
    sliderYear.setValue(0);
    instance.timeNum = Number(sliderYear.getValue()) + 1;

    $("#month-slider").addClass("hidden");
    $("#year-slider").removeClass("hidden");
    $("#day-slider").addClass("hidden");
    $("#day").removeClass("active");
    $('#month').removeClass("active");
    $('#year').addClass("active");

    refresh();
}

function month() {
    $("#month-slider").removeClass("hidden");
    $("#year-slider").addClass("hidden");
    $("#day-slider").addClass("hidden");
    $("#day").removeClass("active");
    $('#month').addClass("active");
    $('#year').removeClass("active");

    instance.timeTypeDisp = "月";
    instance.timeType = "month"
    sliderMonth.setValue(0);
    instance.timeNum = Number(sliderMonth.getValue()) + 1;
    refresh();
}


//new
var pageSize = 4;
var firstTime = true;
var newInstanceInfo = {};
var providerList = [];
var regionList = [];
var imagePublicList = [];
var imagePrivateList = [];
var imageGroupList = [];
var totalImagePage;
var osList = [];
var instanceTypeList = [];
var bandWidthList = [];
var hardDiskList = [];
var securityGroupList = [];
var instance = {
    provider: null,
    region: null,
    zone: null,
    image: null,
    os: null,
    securityGroup: null,
    instanceType: null,
    hardDiskSys: null,
    maxhardDiskAdded: 1,
    hardDiskAdded: [],
    bandWidth: 0,
    num: 1,
    timeType: "year",
    timeTypeDisp: "年",
    timeNum: 1,
    hostName: "root",
    password: null,
    settingLater: false
}




function getInfo() {
    $.ajax({
        type: "GET",
        url: "vm/getNewInstanceInfo",
        dataType: "json",
        data:{appname:$('#yunhai_appname').val()},
        success: function (data) {
            newInstanceInfo = data;
            providerList = data.providerList;
            regionList = data.regionList;
            imagePublicList = data.imagePublicList;
            imagePrivateList = data.imagePrivateList;
            imageGroupList = data.imageGroupList;
            instanceTypeList = data.instanceTypeList;
            securityGroupList = data.securityGroupList;
            // osList = data.imageInfo.osList;
            zoneList = data.zoneList;
            bandWidthList = data.bandWidthList;
            hardDiskList = data.hardDiskList;
            console.log(hardDiskList);
            //对结果进行排序
            instanceTypeList.sort(function (a, b) {
                return a.cpuCoreCount - b.cpuCoreCount;
            })
            bandWidthList.sort(function (a, b) {
                return a.bandWidth - b.bandWidth;
            });
            hardDiskList.sort(function (a, b) {
                return a.hardDisk - b.hardDisk;
            });


            //貌似又是slider的一个bug
            // sliderBandWidth.setValue(0);
            day();
            showImage();
            // refresh();
        }
    });
}
//创建虚拟机页面首先给出4个公共镜像供用户使用
function showImage() {
    var count = 0;
    var list = $("#image-public");
    var type = "public";
    list.html("");
    for (var i = 0; i < imagePublicList.length; i++) {
        var item = $(
                "<div class='col-md-3'>" +
                "<div class='panel panel-default front-panel' id='"+i+"kuang' style='margin-bottom: 0px;'>"+
                "<input class='hidden' id='"+i+"Item' value='"+i+type+"'>"+
                "<div class='panel-heading'>" + imagePublicList[i].name + "</div>" +
                "<div class='panel-body'>" +
                "<p>" + "操作系统:" + imagePublicList[i].os + "</p>" +
                "<p>" + "默认密码:" + imagePublicList[i].code + "</p>" +
                "<a href='javascript:void(0)' onclick='showImageDescription(" + "\"" +imagePublicList[i].description + "\"" + ","+ "\"" +imagePublicList[i].software + "\""+","+ "\""+imagePublicList[i].code+ "\""+","+ "\""+imagePublicList[i].name+ "\"" +")'>" + "更多" + "</a>" +
                "<div>" +
                "<a id=" +i+type  +" class='btn " + i+type +" btn-default float-right' onclick='changeImage("+ "\"" + +i+type +"\""+"," +i +","+"\""+type+"\""+")'>" + "选择" + "</a>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>");
        list.append(item);
        count++;
        if(count >=4) break;
    }
}
//展示镜像的详情信息（只有创建虚拟机首页存在，浮层中不存在）
function showImageDescription(description,software,code,name) {
    var edithref = "vm/newvm/_descriptionimage.jsp?description="+description+"&software="+software+"&code="+code+"&name="+name;
    $.frontModal({
        size: 'modal-md',
        title: '云镜像详情',
        href: edithref
    }).on('show.bs.modal',function () {})
}
//弹出浮层，提供镜像市场
function showMoreImage() {
    var href = "vm/newvm/_imagemarket.jsp";
    moreImageModal = $.frontModal({
        size: 'modal-lg',
        title: '镜像市场',
        href: href
    }).on('show.bs.modal',function () {

    })
}
//
function changeImageType(type) {
    var list = $("#image-detail");
    list.html("");
    var imageList;
    if (type == 'private') {
        imageList = imagePrivateList;
    }
    else if (type == 'public') {
        imageList = imagePublicList;
    }
    else if (type == 'group') {
        imageList = imageGroupList;
    }
    return imageList;
}

//镜像市场的分页代码
function showImageDiv(type,page) {
    $("#private-image").removeClass('active');
    $("#public-image").removeClass('active');
    $("#group-image").removeClass('active');
    $("#"+type+"-image").addClass('active');
    var length;
    var imageList = changeImageType(type);
    if (imageList.length==0) {
        return;
    }
    var list = $("#image-detail");
    totalImagePage = Math.ceil(imageList.length/pageSize);
    if(page == totalImagePage) {
        length = imageList.length
    }
    else {
        length = pageSize*page;
    }
    for (var i = (page-1)*pageSize; i < length; i++) {
        item = acquireImageDetail(imageList[i],type,i);
        list.append(item);
    }
    $("#image_divpage").html($.getDivPageHtml(page, totalImagePage, "get"+type));
}

function getprivate(page) {
    showImageDiv("private",page)
}
function getpublic(page) {
    showImageDiv("public",page)
}
function getgroup(page) {
    showImageDiv("group",page)
}

function acquireImageDetail(image,type,index) {
    var item = $(
        "<div class='col-md-6'>" +
        "<div class='panel panel-default front-panel' style='margin-bottom: 30px'>" +
        "<div class='panel-heading'>" + image.name + "</div>" +
        "<div class='panel-body'>" +
        "<p>" + "镜像名称:" + image.name + "</p>" +
        "<p>" + "操作系统:" + image.os + "</p>" +
        "<p>" + "默认密码:" + image.code + "</p>" +
        "<p>" + "镜像描述:" + image.description + "</p>" +
        "<p>" + "安装软件:" + image.software + "</p>" +
        "<div>" +
        "<a id=" +index+type  +" class='btn "+index+type+" btn-default float-right' onclick='changeImageInMarket("+ "\"" + +index+type +"\""+"," + index + "," + "\"" + type + "\"" + ",\""+image.name+"\""+ ",\""+image.os+"\""+ ",\""+image.code+"\""+ ",\""+image.description+"\""+ ",\""+image.software+"\")'>" + "选择" + "</a>" +
        "</div>" +
        "</div>" +
        "</div>" +
        "</div>");
    return item;
}

var lastid;
function changeImage(id,index,type) {
    if(lastid != null) $("."+lastid).removeClass('active');
    $("."+id).addClass('active');
    lastid = id;
    if(type == 'public') {
        instance.image = imagePublicList[index];
        instance.os = instance.image.os;
    }
    else if(type == 'private') {
        instance.image = imagePrivateList[index];
        instance.os = instance.image.os;
    }
    else if(type == 'group') {
        instance.image = imageGroupList[index];
        instance.os = instance.image.os;
    }
    $("#0kuang").css("border","#ddd 1px solid");
    $("#1kuang").css("border","#ddd 1px solid");
    $("#2kuang").css("border","#ddd 1px solid");
    $("#3kuang").css("border","#ddd 1px solid");
    $("#"+id.charAt(0)+"kuang").css("border","#969696 2px solid");
    //$("#"+id+"kuang").style.borderColor="#FF0000";
    //background
    $("#choosedImage").text(instance.image.name)
    refresh();
}

function changeImageInMarket(id,index,type,name,os,code,description,software) {
    var item0 = $('#0Item').val();
    var item1 = $('#1Item').val();
    var item2 = $('#2Item').val();
    var item3 = $('#3Item').val();
    if(id!=item0&&id!=item1&&id!=item2&&id!=item3){
        var item = "<input class='hidden' id='0Item' value='"+index+type+"'>"+
            "<div class='panel-heading'>" + name + "</div>" +
            "<div class='panel-body'>" +
            "<p>" + "操作系统:" + os + "</p>" +
            "<p>" + "默认密码:" + code + "</p>" +
            "<a href='javascript:void(0)' onclick='showImageDescription(" + "\"" +description + "\"" + ","+ "\"" +software + "\""+","+ "\""+code+ "\""+","+ "\""+name+ "\"" +")'>" + "更多" + "</a>" +
            "<div>" +
            "<a id=" +index+type  +" class='btn " + index+type +" btn-default float-right' onclick='changeImage("+ "\"" + +index+type +"\""+"," +index +","+"\""+type+"\""+")'>" + "选择" + "</a>" +
            "</div>" +
            "</div>";
        $('#0kuang').html(item);
    }
    if(lastid != null) $("."+lastid).removeClass('active');
    $("."+id).addClass('active');
    lastid = id;
    if(type == 'public') {
        instance.image = imagePublicList[index];
        instance.os = instance.image.os;
    }
    else if(type == 'private') {
        instance.image = imagePrivateList[index];
        instance.os = instance.image.os;
    }
    else if(type == 'group') {
        instance.image = imageGroupList[index];
        instance.os = instance.image.os;
    }
    $("#1kuang").css("border","#ddd 1px solid");
    $("#2kuang").css("border","#ddd 1px solid");
    $("#3kuang").css("border","#ddd 1px solid");
    $("#0kuang").css("border","#969696 2px solid");
    $("#choosedImage").text(instance.image.name)
    refresh();
}

function hideModel() {
    $(moreImageModal).modal('hide');
}


function refresh() {
    refreshProvider();
    refreshRegion();
    refreshZone();
    // refreshImage();
    // refreshImage();
    refreshSecurityGroup();
    refreshInstanceType();
    refreshHardDisk();
    refreshTime();
    refreshBandWidth();
    refreshInstanceInfo();
    //创建主机的按钮
    if (check()) {
        $("#btn-new-instance").removeAttr("disabled");
    } else {
        $("#btn-new-instance").attr("disabled", "disabled");
    }

    //计算主机的价格
    $("#instance-price").html(countPrice());

    $(".loading").css("display", "none");
    $(".show-later").removeClass("hidden").removeClass("show-later");
}

function refreshProvider() {}

function refreshInstanceInfo() {
    instance.instanceName = $("#instancename").val();
    instance.hostName = $("#hostname").val();
}

function refreshSecurityGroup() {
    if (instance.securityGroup == null) {
        $("#security-disp").html("请选择安全组");
        $("#instance-security").html("请选择安全组");
        $("#instance-security").addClass("red");
    } else {
        $("#security-disp").html(instance.securityGroup.groupName);
        $("#instance-security").html(instance.securityGroup.groupName);
        $("#instance-security").removeClass("red");
    }

    $("#security-list").html("");
    for (var i = 0; i < securityGroupList.length; i++) {
        var item = securityGroupList[i];
        var $item = $("<li><a onclick='chengeSecurityGroup(" + i + ")'>" + item.groupName + "</a></li>");
        $("#security-list").append($item);
    }
}

function refreshInstanceType() {
    $("#instanceType-list").html("");
    for (var i = 0; i < instanceTypeList.length; i++) {
        var item = instanceTypeList[i];
        if (instance.region.regionId == item.regionId) {
            var text = item.cpuCoreCount + "核" + item.memorySize + "G(" + item.instanceTypeId + ")";
            var $item = $("<li><a onclick='changeInstanceType(" + i + ")'>" + text + "</a></li>");
            $("#instanceType-list").append($item);

            //如果为空
            if (instance.instanceType == null) {
                instance.instanceType = instanceTypeList[0];
            }
            //显示已选中项
            if (instance.instanceType == item) {
                $("#instanceType-disp").html(text);

                //右边的选择栏
                $("#instance-type").html(text);
            }
        }
    }
}

function refreshHardDisk() {
    //系统盘相关信息
    instance.hardDiskSys = instance.instanceType.hardDisk;
    $("#hardDisk-sys-disp").html(instance.instanceType.hardDisk + "G");
    $("#hardDisk-sys-list").html("<li><a>" + instance.instanceType.hardDisk + "G</a></li>");

    //附加盘信息
    $("#added-disk-list>*:not(:first,:last)").remove();
    var $model = $("#added-disk-list>.hidden");
    for (var i = 0; i < instance.hardDiskAdded.length; i++) {
        var hardDisk = instance.hardDiskAdded[i];
        var $item = $model.clone(true);
        $item.removeClass("hidden");
        $item.find(".hardDisk-added-disp").html(hardDisk.hardDisk + "G");
        $item.find(".del-hardDisk").attr("onclick", "delHardDisk(" + i + ")");
        $item.find(".hardDisk-added-list").html("");
        for (var j = 0; j < hardDiskList.length; j++) {
            var $item2 = $("<li><a onclick='changeAddedHardDisk(" + i + "," + j + ")'>" + hardDiskList[j].hardDisk + "G</a> </li>");
            $item.find(".hardDisk-added-list").append($item2);
        }
        $model.after($item);
    }

    $("#hardDiskAdded-left").html(instance.maxhardDiskAdded - instance.hardDiskAdded.length);
    if (instance.maxhardDiskAdded - instance.hardDiskAdded.length <= 0) {
        $("#hardDiskAdded-left").parent().parent().hide();
    } else {
        $("#hardDiskAdded-left").parent().parent().show();
    }

    //右边的信息栏
    var added = 0;
    for (var i = 0; i < instance.hardDiskAdded.length; i++) {
        added += Number(instance.hardDiskAdded[i].hardDisk);
    }
    $("#instance-disk").html(instance.hardDiskSys + "G+" + added + "G");

}

function refreshBandWidth() {
    if (firstTime) {
        instance.bandWidth = bandWidthList[0];

        var text = "[";
        var text2 = "[";
        for (var i = 0; i < bandWidthList.length - 1; i++) {
            text += i + ",";
            text2 += "\"" + bandWidthList[i].bandWidth + "Mbps\",";
        }
        text += bandWidthList.length - 1 + "]";
        text2 += "\"" + bandWidthList[bandWidthList.length - 1].bandWidth + "Mbps\"" + "]";

        $("#bandWidth").attr("data-slider-ticks", text);
        $("#bandWidth").attr("data-slider-ticks-labels", text2);
        $("#bandWidth").parent().find("*:first").remove();
        // $("#bandWidth").slider();
        sliderBandWidth = new Slider("#bandWidth");
        sliderBandWidth.setValue(0);
        firstTime = false;
    }

    instance.bandWidth = bandWidthList[$("#bandWidth").attr("value")];
    $("#instance-band").html("带宽 " + instance.bandWidth.bandWidth + "Mbps");
    $("#bandWidth-label").html(instance.bandWidth.bandWidth);
}

function refreshTime() {
    $("#instance-time").html(instance.timeNum + instance.timeTypeDisp + "×" + instance.num + "台");
}

function refreshRegion() {
    $("#region-list").html("");
    for (var i = 0; i < regionList.length; i++) {
        var region = regionList[i];
        var $item = $("<a class=\"btn btn-default front-no-box-shadow\" onclick='changeRegion(" + i + ")'>" +
            region.regionName + "</a>");
        if (instance.region == null) {
            instance.region = regionList[0];
        }
        if (region.regionId == instance.region.regionId) {
            $item.addClass("active");
        }
        $("#region-list").append($item);
    }
}

function refreshZone() {
    $("#zone-list").html("");
    for (var i = 0; i < zoneList.length; i++) {
        var zone = zoneList[i];
        if (zone.regionId = instance.region.regionId) {
            var $item = $("<li><a onclick='changeZone(" + i + ")'>" + zone.zoneName + "</a></li>");
            if (instance.zone == null) {
                instance.zone = zone;
            }
            if (zone.zoneId == instance.zone.zoneId) {
                $("#zone-disp").html(zone.zoneName);
            }
            $("#zone-list").append($item);
        }
    }

    //右边信息栏
    $("#instance-region").html(instance.region.regionName + "(" + instance.zone.zoneName + ")");
}

function changeRegion(index) {
    instance.region = regionList[index];
    instance.zone = null;
    refresh();
}

function changeZone(index) {
    instance.zone = zoneList[index];
    refresh();
}

function changeOS(index) {
    $('#image-dropdown').removeClass('disabled');
    instance.os = osList[index];
    instance.image = null;
    refresh();
}

function changeTime(num) {
    instance.timeNum = Number(num) + 1;
    refresh();
}

function changeBand(num) {
    instance.bandWidth = num;
    $("#bandwidth-label").text(num);
    refresh();
}

function changeInstanceType(num) {
    instance.instanceType = instanceTypeList[num];
    refresh();
}

function changeNum(num) {
    instance.num += num;
    if (instance.num <= 0) instance.num = 1;
    $("#num").html(instance.num);
    refresh();
}

function changeAddedHardDisk(i, j) {
    instance.hardDiskAdded[i] = hardDiskList[j];
    refresh();
}

function addHardDisk() {
    if (instance.hardDiskAdded.length < instance.maxhardDiskAdded) {
        instance.hardDiskAdded.push(hardDiskList[0]);
    }
    refresh();
}

function delHardDisk(num) {
    instance.hardDiskAdded.splice(num, 1);
    refresh();
}

function chengeSecurityGroup(num) {
    instance.securityGroup = securityGroupList[num];
    refresh();
}

function changePassword() {
    if ($("#password").val() != "") {
        if ($("#password").val().length < 8) {
            instance.password = null;
            $("#password-error2").removeClass("hidden");
        } else {
            $("#password-error2").addClass("hidden");
        }
        if ($("#password").val() == $("#password2").val()) {
            instance.password = $("#password").val();
            $("#password-error").addClass("hidden");
        } else {
            instance.password = null;
            $("#password-error").removeClass("hidden");
        }
    } else {
        instance.password = null;
        $("#password-error").addClass("hidden");
    }

    refresh();
}


//创建主机的逻辑
function newInstance() {
    instance.securityGroup = securityGroupList[0];
    //硬盘的逻辑
    var dataDisk = null;
    console.log(instance.provider)
    if (instance.hardDiskAdded.length != 0) dataDisk = instance.hardDiskAdded[0].hardDisk;
    else dataDisk = "0";

    if (check()) {
        $.fillTipBox({type:'success',icon:"glyphicon-ok-sign",content:'主机申请中，请勿重复点击！'})
        $.ajax({
            method: "POST",
            url: "vm/newInstance",
            type: "json",
            data: {
                appname:$('#yunhai_appname').val(),
                provider: 'yunhai',
                regionId: instance.region.regionId,
                zoneId: $("#yunhai_ulZone").val(),
                instanceTypeId: instance.instanceType.instanceTypeId,
                imageId: instance.image.id,
                securityGroupId: instance.securityGroup.groupId,
                instanceName: instance.instanceName,
                password: instance.password,
                instanceChargeLength: instance.timeNum,
                instanceChargeType: instance.timeType,
                instanceNum: instance.num,
                bandWidth: instance.bandWidth.bandWidth,
                hostName: instance.hostName,
                dataDisk: dataDisk
            },
            success: function (data) {
                $.fillTipBox({type:"success",icon:"glyphicon-ok-sign",content:data.info});
                if (data.result = "success") {
                    window.location.href = "vm/vmlist.jsp";
                }
            }
        });
    }

}

function check() {
    if (instance.region == null) {
        return false;
    }
    if (instance.zone == null) {
        return false;
    }
    if (instance.image == null) {
        return false;
    }
    if (instance.os == null) {
        return false;
    }
    if (instance.instanceType == null) {
        return false;
    }
    if (instance.hardDiskSys == null) {
        return false;
    }
    if (instance.securityGroup == null) {
        instance.securityGroup = securityGroupList[num];
    }
    if (!$("#protocal-checkbox")[0].checked) {
        return false;
    }

    if (instance.settingLater == false) {
        if (instance.hostName == "") {
            return false;
        }

        if (instance.instanceName == "") {
            return false;
        }

        if (instance.password == null) {
            return false;
        }
    } else {
        instance.hostName = null;
        // instance.instanceName = null;
        instance.password = null;
    }

    return true;
}

function countPrice() {
    var payType;
    switch (instance.timeTypeDisp) {
        case "年":
            payType = "yearPrice";
            break;
        case "月":
            payType = "monthPrice";
            break;
        case "天":
            payType = "dayPrice";
            break;
        default:
            payType = "yearPrice";
    }

    //计算硬盘的价格
    var hardDiskPrice = 0;
    for (var i = 0; i < instance.hardDiskAdded.length; i++) {
        var hardDisk = instance.hardDiskAdded[i];
        hardDiskPrice += Number(hardDisk[payType]);
    }
    var basePrice = Number(instance.instanceType[payType]) + Number(instance.bandWidth[payType]) + hardDiskPrice;
    var finalPrice = basePrice * instance.num * instance.timeNum / 100;
    return finalPrice;
}
