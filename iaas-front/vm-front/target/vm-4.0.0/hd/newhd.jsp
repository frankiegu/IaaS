<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<!DOCTYPE html>
<html>
<head>
<title>申请云硬盘</title>
<base
	href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() %>/" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<s:include value="../template/_head.jsp" />
<style>
  	.slider-tick {
  		opacity: 0 !important;
  	}
  	.slider-selection {
		background-image: -webkit-linear-gradient(top,#89cdef 0,#81bfde 100%);
	    background-image: -o-linear-gradient(top,#89cdef 0,#81bfde 100%);
	    background-image: linear-gradient(to bottom,#89cdef 0,#81bfde 100%);
	    background-repeat: repeat-x;
  	}
  </style>
</head>
< class="front-body">
	<s:include value="../template/_banner.jsp?menu=newhd" />
	<div class="front-inner">
		<div class="container">
			<div class="fix-gutter">
				<div class="row">
					<div class="col-md-12">
						<ul class="breadcrumb">
							<li><a href="hd/hd_list.jsp?menu=hd">云硬盘</a></li>
							<li class="active">申请云硬盘</li>
						</ul>
					</div>
				</div>
				<div class="row">
					<div class="col-md-9">
						<div class="panel-body panel-default front-panel"
							style="margin: -10px -15px -15px -15px;">
							<div class="panel-body front-no-padding">
								<table class="table table-striped front-table"
									style="margin-bottom: 0px;">

									<tr>
										<div class="panel panel-default front-panel card-gutter">
											<div class="panel-heading">基本设置</div>
											<div class="panel-body front-last-no-margin">
												<div class="loading">
	                                                <div class="front-loading">
	                                                    <div class="front-loading-block"></div>
	                                                    <div class="front-loading-block"></div>
	                                                    <div class="front-loading-block"></div>
	                                                </div>
	                                                <div class="panel-body text-center">正在加载请稍候</div>
                                          		</div>
												<div class="row hidden show-later">
													<div class="col-md-2 col-sm-3 col-xs-4">
														<h5 class="label-pos2">提供商</h5>
													</div>
													<div class="col-md-10 col-sm-9 col-xs-8 button-group-align">
														<div class="front-toolbar other">
															<div class="front-toolbar-header clearfix">
																<button type="button"
																	class="front-toolbar-toggle navbar-toggle"
																	data-toggle="collapse" data-target="#provider">
																	<span class="icon-bar"></span> <span class="icon-bar"></span>
																	<span class="icon-bar"></span>
																</button>
															</div>
															<div id="provider" class="front-btn-group collapse">
																<a class="btn btn-default front-no-box-shadow active">云海</a>

															</div>
														</div>
													</div>
												</div>
												<div class="row hidden show-later">
													<div class="col-md-2 col-sm-3 col-xs-4">
														<h5 class="label-pos2">地域</h5>
													</div>
													<div class="col-md-10 col-sm-9 col-xs-8 button-group-align">
														<div class="front-toolbar other">
															<div class="front-toolbar-header clearfix">
																<button type="button"
																	class="front-toolbar-toggle navbar-toggle"
																	data-toggle="collapse" data-target="#region">
																	<span class="icon-bar"></span> <span class="icon-bar"></span>
																	<span class="icon-bar"></span>
																</button>
															</div>
															<div id="region-list" class="front-btn-group collapse">
																<a class="btn btn-default front-no-box-shadow active">北京</a>
															</div>
														</div>
													</div>
												</div>
												<div class="row hidden show-later">
													<div class="col-md-2">
														<h5 class="label-pos2">可用区</h5>
													</div>
													<div class="col-md-10">
														<div class="btn-group">
															<button id="zonebtn" type="button" class="btn btn-default">zpark</button>
															<button type="button"
																class="btn btn-default dropdown-toggle"
																data-toggle="dropdown" aria-haspopup="true"
																aria-expanded="false">
																<span class="caret"></span> <span class="sr-only">Toggle
																	Dropdown</span>
															</button>
															<ul class="dropdown-menu" id="zone-list">
																<li><a href="javascript:void(0)">zpark</a></li>
															</ul>
														</div>
													</div>
												</div>
												<div class="row hidden show-later" style="margin-top:10px">
													<div class="col-md-2">
														<h5 class="label-pos2">硬盘名称</h5>
													</div>
													<div class="col-md-10">
														<input id="diskname" style="width:50%" type="text" class="form-control col-md-5" placeholder="请输入硬盘名称">
													</div>
												</div>
											</div>
										</div>
									</tr>
									<tr>
										<div class="panel panel-default front-panel card-gutter">
											<div class="panel-heading">配置</div>
											<div class="panel-body front-last-no-margin">
												<%--加载等待动画--%>
	                                            <div class="loading">
	                                                <div class="front-loading">
	                                                    <div class="front-loading-block"></div>
	                                                    <div class="front-loading-block"></div>
	                                                    <div class="front-loading-block"></div>
	                                                </div>
	                                                <div class="panel-body text-center">正在加载请稍候</div>
	                                            </div>
	                                            <%--加载等待动画--%>
												<div class="row hidden show-later">
													<div class="col-md-2 col-sm-12 col-xs-12">
														<h5 class="label-pos2">类型</h5>
													</div>
													<div class="col-md-3 col-sm-12 col-xs-12">
														<div class="btn-group">
															<button type="button" class="btn btn-default">普通云盘</button>
															<button type="button"
																class="btn btn-default dropdown-toggle"
																data-toggle="dropdown" aria-haspopup="true"
																aria-expanded="false">
																<span class="caret"></span> <span class="sr-only">Toggle
																	Dropdown</span>
															</button>
															<ul class="dropdown-menu">
																<li><a href="javascript:void(0)">普通云盘</a></li>
															</ul>
														</div>
													</div>
													<div class="col-md-3 col-sm-2 col-xs-4">
                                                        <div class="btn-group">
                                                            <button type="button"
                                                                    class="btn btn-default hardDisk-added-disp"><span id="diskbtn">20</span>G
                                                            </button>
                                                            <button type="button"
                                                                    class="btn btn-default dropdown-toggle"
                                                                    data-toggle="dropdown" aria-haspopup="true"
                                                                    aria-expanded="false">
                                                                <span class="caret"></span>
                                                                <span class="sr-only">Toggle Dropdown</span>
                                                            </button>
                                                            <ul class="dropdown-menu hardDisk-added-list" id="disksize">
                                                                <li><a href="javascript:void(0)">30G</a></li>
                                                            </ul>
                                                        </div>
                                                    </div>
												</div>
											</div>
										</div>
									</tr>
									<tr>
									<div class="panel panel-default front-panel card-gutter">
											<div class="panel-heading">配置</div>							
										<div id="goodprice" class="panel-body front-last-no-margin" style="z-index:-1">
											<%--加载等待动画--%>
                                            <div class="loading">
                                                <div class="front-loading">
                                                    <div class="front-loading-block"></div>
                                                    <div class="front-loading-block"></div>
                                                    <div class="front-loading-block"></div>
                                                </div>
                                                <div class="panel-body text-center">正在加载请稍候</div>
                                            </div>
                                            <%--加载等待动画--%>
											<div class="row hidden show-later">
												<div class="col-md-2 col-sm-3 col-xs-4">
													<h5 class="label-pos2">付费方式</h5>
												</div>
												<div class="col-md-10 col-sm-9 col-xs-8 button-group-align">
													<div class="front-toolbar other">
														<div class="front-toolbar-header clearfix">
															<button type="button"
																class="front-toolbar-toggle navbar-toggle"
																data-toggle="collapse" data-target="#payment">
																<span class="icon-bar"></span> <span class="icon-bar"></span>
																<span class="icon-bar"></span>
															</button>
														</div>
														<div id="payment" class="front-btn-group collapse">
															<button id="year" value="PayByYear" class="btn btn-default front-no-box-shadow active" onclick="year(yearPrice)">包年</button>
															<button id="month" value="PayByMonth" class="btn btn-default front-no-box-shadow" onclick="month(monthPrice)">包月</button> 
															<button id="day" value="PayByDay" class="btn btn-default front-no-box-shadow" onclick="day(dayPrice)">包天</button> 
														</div>
													</div>
												</div>
											</div>
											<div class="row show-later" style="margin-top: 20px">
												<div class="col-md-2 label-gutter">
													<h5 class="label-pos2">购买时长</h5>
												</div>
												<div id="slider_parent" class="col-md-10" style="padding-left: 26px;height:40px">
													<!-- 按年续费 -->
													<div id="year-slider" class="front-slider noselect">
													    <input id="year-ex" style="width:60%" data-slider-handle="round" type="text" 
													    	data-slider-ticks="[1,2,3]"
													    	data-slider-ticks-labels='["1", "2", "3"]'
													    	data-slider-min="1" data-slider-max="3" data-slider-step="1" data-slider-value="0" data-slider-tooltip="hide"/>
													</div>
													<!-- 按月续费 -->
													<div id="month-slider" class="front-slider noselect">
													    <input id="month-ex" style="width:80%" data-slider-handle="round" type="text" 
													    	data-slider-ticks="[1,2,3,4,5,6,7,8,9,10,11,12]"
													    	data-slider-ticks-labels='["1", "2", "3","4","5","6","7","8","9","10","11","12"]'
													    	data-slider-min="1" data-slider-max="12" data-slider-step="1" data-slider-value="0" data-slider-tooltip="hide"/>
													</div>	
													<!-- 按日续费-->
													<div id="day-slider" class="front-slider noselect">
													    <input id="day-ex" style="width:90%" data-slider-handle="round" type="text" 
													    	data-slider-ticks="[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]" 
													    	data-slider-ticks-labels='["1", "2", "3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"]'
													    	data-slider-min="1" data-slider-max="20" data-slider-step="1" data-slider-value="0" data-slider-tooltip="hide"/>
													</div>					
												</div>			
											</div>
										</div>
										<div class="row show-later" style="margin-bottom: 18px">
                                                <div class="col-md-2 label-gutter">
                                                    <h5 class="label-pos2">数量</h5>
                                                </div>
                                                <div class="col-md-10" style="padding-top: 8px">
														<span id="plus" style="cursor: pointer"
                                                              class="glyphicon glyphicon-plus"></span>
                                                    <div style="display: inline-block; margin-left: 4px; margin-right: 4px;">
                                                        <span class="slider-label" disabled="disabled" id="num">1</span><span>&nbsp;&nbsp;台</span>
                                                    </div>
														<span id="minus" style="cursor: pointer"
                                                              class="glyphicon glyphicon-minus"></span>
                                                </div>
                                        </div>
									</div>
									</tr>
								</table>
							</div>
						</div>
					</div>
					<div class="col-md-3 col-xs-12 col-sm-12 right-card" >

						<div class="panel panel-default front-panel"
							style="margin-top: 5px">
							<div class="panel-heading">购买清单</div>
						</div>
						<div class="panel panel-default front-panel"
							style="margin-top: -8px">
							<div class="panel-heading">当前配置</div>
							<div class="panel-body front-last-no-margin">
								<%-- <div class="row">
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="label-pos">硬盘名称</div>
									</div>
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="content-gutter"><span id="rightname">unknow-name</span></div>
									</div>
								</div> --%>
								<div class="row">
									<div class="col-md-4 col-sm-6 col-xs-6 right-label">
										<div class="label-pos">提供商</div>
									</div>
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="content-gutter"><span id="rightprovider">云海</span></div>
									</div>
								</div>
								<div class="row">
									<div class="col-md-4 col-sm-6 col-xs-6 right-label">
										<div class="label-pos">地域</div>
									</div>
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="content-gutter"><span id="rightregion">北京</span>(<span id="rightzone">spark</span>)</div>
									</div>
								</div>
								
								
								<div class="row">
									<div class="col-md-4 col-sm-6 col-xs-6 right-label">
										<div class="label-pos">配置</div>
									</div>
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="content-gutter"><span id="rightdisk">20</span>G</div>
									</div>
								</div>
								
								<div class="row">
									<div class="col-md-4 col-sm-6 col-xs-6 right-label">
										<div class="label-pos">购买量</div>
									</div>
									<div class="col-md-8 col-sm-6 col-xs-6 right-label">
										<div class="content-gutter"><span id="timenum">1</span><span id="paytype">年</span> × <span id="goodnum">1</span>台</div>
									</div>
								</div>
								<div class="row">
									<div class="col-md-12 col-sm-7 col-xs-6 right-label">
										<div style="margin-top: -10px; margin-left: 25px">
											<div class="checkbox">
												<input id="paycheck" type="checkbox">
											</div>
											<span>我同意</span><a href="javascript:void(0)" onclick="yunhai_term()">云海服务条款</a>
										</div>
									</div>
								</div>
								<%--<div class="row">
									<div class="col-md-5 right-label">
										<div style="margin-left: 5px">配置费用</div>
									</div>
								</div>
								<div class="row">
									<div
										class="col-md-offset-3 col-sm-offset-4 col-xs-offset-4 col-md-1 col-sm-1 col-xs-1">
										<img src="images/big-money.png" />
									</div>
									<div class="col-md-2 col-sm-2 col-xs-2">
										<span id="price"
											style="font-size: 32px; color: #337ab7; margin-left: -2px">100</span>
									</div>
								</div>--%>
								<div class="row">
									<div class="col-md-offset-4 col-sm-offset-5 col-xs-offset-5">
										<input id="appname" type="hidden" value="${param.appname}"/>
										<button id="paydisk" type="button" disabled="disabled" onclick="createDisk('${param.appname}')" class="btn btn-primary">立即购买</button>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		<s:include value="../template/_footer.jsp" />
	</div>
	<s:include value="../template/_global.jsp" />
	<script src="js/hd/newhd.js"></script>
	<s:include value="../vm/term_of_service.jsp"/>
</html>
</body>
</html>