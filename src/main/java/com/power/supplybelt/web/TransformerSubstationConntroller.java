package com.power.supplybelt.web;

import com.power.supplybelt.entity.*;
import com.power.supplybelt.util.Constants;
import com.power.supplybelt.util.DMQuery;
import com.power.supplybelt.util.DMUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("transformer")
public class TransformerSubstationConntroller {

    private static Logger logger = LoggerFactory.getLogger(TransformerSubstationConntroller.class);
    
    //供带文件存放路径
    @Value("${supplybelt.file.path}")
    private String supplybeltFilePath;
    
    //数据展示空格用
    String spaceString = "        ";

//    //debug 查询时刻
//    private static String queryMoment = "2018-10-18 14:53:50";
    //debug 查询时刻
    private static String queryMoment = "2018-10-18 14:53:50";
    
    //是否查询负载
    private boolean queryLoadForResult = false;
    
    //断路器
    public static List<Breaker> breakerList = new ArrayList<>();
    //原始线端数据，没有过滤的
    public static List<ACLineEnd> acLineEndList = new ArrayList<>();
    //线段列表
    public static  List<ACLineSegment> acLineSegmentList = new ArrayList<>();
    // 由端同间隔的断路器状态过滤后的线端设备
    public static List<ACLineEnd> filterByBreakerEndList = new ArrayList<>();
    // 根据线端和线段一起确定的数据结构
    public static List<ACLineEndWithSegment> acLineEndWithSegmentList = new ArrayList<>();
    //过滤后的厂站列表
    public static List<Substation> substationList = new ArrayList<>();
    //变电站类列表
    public static List<TransformerSubstation> transformerSubstationList = new ArrayList<>();
    //过滤后的主变列表
    public static List<PowerTransformer> powerTransformerList = new ArrayList<>();
    //过滤后的绕阻列表
    public static List<TransformerWinding> transformerWindingList = new ArrayList<>();
    //带有绕阻信息的主变
    public static List<PowerTransformerWithWinding> powerTransformerWithWindingList = new ArrayList<>();
    //隔刀列表
    public static List<Disconnector> disconnectorList = new ArrayList<>();
    //母线列表
    public static List<Busbarsection> busbarsectionList = new ArrayList<>();
    //T接线列表
    public static List<ACLineWithSubstation> acLineWithSubstationList = new ArrayList<>();
    //记录最终供带关系列表
    List<PowerSupplyData> powerSupplyData = new ArrayList<>();
    //变成全局对象
    Statement stmt = null;
    //结果集
    ResultSet rs = null;
    //用于记录由于一个T里面会在构建父子时将原本的一条线构建成多条，导致后面供带重复的情况，此处用来记录供带双方
    List<RecordProducerAndConsumer> hasFoundSupplyData = new ArrayList<RecordProducerAndConsumer>();
    //用来记录查过遥测的winding，当前时刻，每次记得清空
    Map<String, String> queryedWindingYC = new HashMap<>();

    
    //debug, 缺失厂站信息
    List<Substation> lackStations = new ArrayList<Substation>();
    List<TransformerSubstation> supplyBeltStationList = new ArrayList<TransformerSubstation>();


    @RequestMapping(value="/YXStatusQueryById", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(notes ="遥信状态查询", value = "遥信状态查询")
    public PowerWebResponse YXStatusQueryById(HttpServletRequest request, String tableId, long columnId,
    String date){

        PowerWebResponse response = new PowerWebResponse();
        try {
            System.out.println("======tableId=" + tableId);
            DMUtils dmUtils = new DMUtils();
            Connection connection = dmUtils.getConnection();
            if (null != connection) {
                // 创建语句对象,最后关闭
                stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                boolean result = YXStatusEqualMoment(date,  tableId, columnId,0, true);
                System.out.println("======date=" + date);
                response.setData(result);
                response.setMsg("遥信状态："+result);
                if (null != stmt) {
                	stmt.close();
				}
                if (null != connection) {
                	connection.close();
				}
                
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  response;
    }

    //---------根据各种参数查找各种设备方法---------------------//
    private Breaker findBreakerByNd(String nd){
        if(null == nd || "-1".equals(nd)){
            return  null;
        }
        for (Breaker breaker: breakerList){
            String jnd = breaker.getJnd();
            String ind = breaker.getInd();
            if (nd.equals(ind) || nd.equals(jnd)){
                return breaker;
            }
        }
        return null;
    }

    private Breaker findBreakerByIndAndJnd(String ind, String jnd){
        if(((null == ind) && (null == jnd))||
        		("-1".equals(ind)&& "-1".equals(jnd))){
            return null;
        }

        //对于ind 和 jnd查找的，如果返回多个，也需要丢弃
        List<Breaker> indFindBreaker =  new ArrayList<>();
        List<Breaker> jndFindBreaker =  new ArrayList<>();
        Breaker result = null;
        for (Breaker breaker: breakerList){
            String indInBreaker = breaker.getInd();
            String jndInBreaker = breaker.getJnd();
            if(null != ind){
                if (ind.equals(indInBreaker) || ind.equals(jndInBreaker)){
                    indFindBreaker.add(breaker);
                }
            }
            if (null != jnd){
                if (jnd.equals(indInBreaker) || jnd.equals(jndInBreaker)){
                    jndFindBreaker.add(breaker);
                }
            }
        }

        //ind找到一条
        if(indFindBreaker.size() == 1  &&
                jndFindBreaker.size() == 0){
            result =  indFindBreaker.get(0);
            System.out.println("通过ind或者jnd找breaker name="+result.getName());
        }

        //jnd找到一条
        if(jndFindBreaker.size() == 1  &&
                indFindBreaker.size() == 0){
            result = jndFindBreaker.get(0);
            System.out.println("通过ind或者jnd找breaker name="+result.getName());
        }

        return null;
    }

    private Breaker findBreakerByBayId(String bayId){
        if(null == bayId){
            return null;
        }
        for (Breaker breaker: breakerList){
            String breakerBayId = breaker.getBayId();
            if (bayId.equals(breakerBayId)){
                return breaker;
            }
        }
        return null;
    }

    private Disconnector findDisconnectorByNd(String nd){
        if((null == nd) ||"-1".equals(nd)){
            return null;
        }
        for (Disconnector disconnector: disconnectorList){
            String ind = disconnector.getInd();
            String jnd = disconnector.getJnd();
            if (nd.equals(ind) || nd.equals(jnd)){
                return disconnector;
            }
        }
        return null;
    }
    
    private Disconnector findDisconnectorByNdAndStIdBvId(String nd, String stId, String bvId){
        if(null == nd || null == bvId || null==stId||"-1".equals(nd)){
            return null;
        }
        for (Disconnector disconnector: disconnectorList){
        	String bvIdDisconnector = disconnector.getBvId();
        	String stIdDisconnector = disconnector.getStId();
            String ind = disconnector.getInd();
            String jnd = disconnector.getJnd();
            String name = disconnector.getName();
            if ((nd.equals(ind) || nd.equals(jnd))&&
            		stId.equals(stIdDisconnector)&&
            		bvId.equals(bvIdDisconnector)&&
            		(null != name && !name.contains("X"))){
            	System.out.println("findDisconnectorByNdAndStIdBvId 找到隔刀");
                return disconnector;
            }
        }
        return null;
    }
    
    private List<Disconnector> findDisconnectorByIndAndJnd(String ind, String jnd){
        if((null==ind && null==jnd) || ("-1".equals(ind) && ("-1".equals(jnd)))){
            return null;
        }

        List<Disconnector> disconnectors = new ArrayList<>();
        for (Disconnector disconnector: disconnectorList){
            String disconnectorInd = disconnector.getInd();
            String disconnectorJnd = disconnector.getJnd();
            if (!"-1".equals(ind) && 
            		(ind.equals(disconnectorInd) || ind.equals(disconnectorJnd))){
            	if (!disconnectors.contains(disconnector)) {
            		disconnectors.add(disconnector);
            		System.out.println("findDisconnectorByIndAndJnd找到隔刀 ind="+ind);
				}
            }
            
            if (!"-1".equals(jnd) && 
            		(jnd.equals(disconnectorInd) || ind.equals(disconnectorJnd))){
              	if (!disconnectors.contains(disconnector)) {
            		disconnectors.add(disconnector);
            		System.out.println("findDisconnectorByIndAndJnd找到隔刀 jnd="+jnd);
				}
            }
        }
        return disconnectors;
    }

    //为了主变和母线确认关系而找的隔刀
    private List<Disconnector> findDisconnectorByBayId(String bayId){
        if(null == bayId){
            return null;
        }

        List<Disconnector> disconnectors = new ArrayList<>();
        for (Disconnector disconnector: disconnectorList){
            String disconnectorBayId = disconnector.getBayId();
            if (bayId.equals(disconnectorBayId)){
                disconnectors.add(disconnector);
            }
        }
        return disconnectors;
    }



    private Busbarsection findBusbarsectionByNd(String nd){
        if(null == nd || "-1".equals(nd)){
            return null;
        }
        for (Busbarsection busbarsection: busbarsectionList){
            String busNd = busbarsection.getNd();
            if (nd.equals(busNd)){
                return busbarsection;
            }
        }
        return null;
    }


    private List<Busbarsection> findBusbarsectionByBvIdAndstId(String bvId, String stId){
        if(null == bvId || null == stId){
            return null;
        }
        List<Busbarsection> busbarsections = new ArrayList<>();
        for (Busbarsection busbarsection: busbarsectionList){
            String bvIdInBus = busbarsection.getBvId();
            String stIdInBus = busbarsection.getStId();
            String name = busbarsection.getName();
            //旁母去除
            if (bvId.equals(bvIdInBus) && stId.equals(stIdInBus)&&
                    (null!=name && !name.contains("旁母"))){
                busbarsections.add(busbarsection);
            }
        }
        return busbarsections;
    }

    private List<Breaker> findBreakerForBusbarsection(String bvId, String stId){
        if(null == bvId || null == stId){
            return null;
        }
        List<Breaker> breakers = new ArrayList<>();
        for (Breaker breaker: breakerList){
            String breakerBvId = breaker.getBvId();
            String breakerStId = breaker.getStId();
            String name = breaker.getName();
            if (bvId.equals(breakerBvId) && stId.equals(breakerStId)&&
                    (null!=name && name.contains("母联"))){
                breakers.add(breaker);
            }
        }
        return breakers;
    }

    //通过母联隔刀关键字找隔刀,对于那种多条母线之间没有断路器的。
    private Disconnector findDisconnetorByNameAndBvIdStId(String bvId, String stId){
        if(null == bvId || null == stId){
            return null;
        }
        for (Disconnector disconnector: disconnectorList){
            String disconnectorBvId = disconnector.getBvId();
            String disconnectorStId = disconnector.getStId();
            String name = disconnector.getName();
            if (bvId.equals(disconnectorBvId) && stId.equals(disconnectorStId)&&
                    (null!=name && name.contains("母联"))){
                System.out.println("findDisconnetorByNameAndBvIdStId name="+name);
                return disconnector;
            }
        }
        return null;
    }


    //在判断主变是否运行时，有绕阻ND=-1的情况找不到隔刀，需要根据编号关键字找
    private Disconnector findDisconnetorByNumberAndBvIdStId(String bvId, String stId, String nameLike){
        if(null == bvId || null == stId || null== nameLike){
            return null;
        }
        for (Disconnector disconnector: disconnectorList){
            String disconnectorBvId = disconnector.getBvId();
            String disconnectorStId = disconnector.getStId();
            String name = disconnector.getName();
            if (bvId.equals(disconnectorBvId) && stId.equals(disconnectorStId)&&
                    (null!=name && name.contains(nameLike))){
                System.out.println("findDisconnetorByNumberAndBvIdStId name="+name);
                return disconnector;
            }
        }
        return null;
    }

    //查找进出线关联的间隔，常规线
    private String findBayIdBySegIdAndStId(String segId, String stId){
        if(null == segId || null == stId){
            return null;
        }

        for(ACLineEndWithSegment acLineEndWithSegment: acLineEndWithSegmentList){
            String aclnsegId = acLineEndWithSegment.getAclnsegId();
            String stIdEnd = acLineEndWithSegment.getStId();
            if (segId.equals(aclnsegId) && stId.equals(stIdEnd)){
                String bayId = acLineEndWithSegment.getBayId();
                System.out.println("findBayIdBySegIdAndStId bayId="+bayId);
                return bayId;
            }
        }
        return null;
    }
    
    
    //查找进出线关联的ND，常规线
    private String findNDBySegIdAndStId(String segId, String stId){
        if(null == segId || null == stId){
            return null;
        }

        for(ACLineEndWithSegment acLineEndWithSegment: acLineEndWithSegmentList){
            String aclnsegId = acLineEndWithSegment.getAclnsegId();
            String stIdEnd = acLineEndWithSegment.getStId();
            if (segId.equals(aclnsegId) && stId.equals(stIdEnd)){
                String nd = acLineEndWithSegment.getNd();
                System.out.println("findBayIdBySegIdAndStId nd="+nd);
                return nd;
            }
        }
        return null;
    }


    //查找进出线关联的间隔，T接线
    private String findBayIdByAclineIdAndStId(String aclineId, String stId){
        if(null == aclineId || null == stId){
            return null;
        }

        String segId = null;
        for(ACLineSegment acLineSegment: acLineSegmentList){
            String aclineIdInSeg = acLineSegment.getAclineId();
            String istId = acLineSegment.getIstId();
            String jstId = acLineSegment.getJstId();
            if (aclineId.equals(aclineIdInSeg) &&
                    (stId.equals(istId) || stId.equals(jstId))){
                //找到T接线中需要的线段
                segId = acLineSegment.getId();
                break;
            }
        }
        //然后再调用前面的方法找bayId
        return findBayIdBySegIdAndStId(segId, stId);
    }
    
    

    //查找进出线关联的ND，T接线
    private String findNDByAclineIdAndStId(String aclineId, String stId){
        if(null == aclineId || null == stId){
            return null;
        }

        String segId = null;
        for(ACLineSegment acLineSegment: acLineSegmentList){
            String aclineIdInSeg = acLineSegment.getAclineId();
            String istId = acLineSegment.getIstId();
            String jstId = acLineSegment.getJstId();
            if (aclineId.equals(aclineIdInSeg) &&
                    (stId.equals(istId) || stId.equals(jstId))){
                //找到T接线中需要的线段
                segId = acLineSegment.getId();
                break;
            }
        }
        //然后再调用前面的方法找ND
        return findNDBySegIdAndStId(segId, stId);
    }


    //根据stId查找站名
    private String findStationNameByStId(String stId){
        if (null == stId){
            return null;
        }
        for(Substation substation: substationList){
            String stIdSub = substation.getId();
            String subName = substation.getName();
            if (stId.equals(stIdSub)){
                return subName;
            }
        }
        return null;
    }
    
    //根据name找站
    private TransformerSubstation findStationByName(String name){
        if (null == name){
            return null;
        }
        for(TransformerSubstation transformerSubstation: allNode220Filter){
            String stationName = transformerSubstation.getName();
            if (stationName.contains(name)){
                return transformerSubstation;
            }
        }
        return null;
    }


    //根据stId查找电压
    private String findStationBvIdByStId(String stId){
        if (null == stId){
            return null;
        }
        for(Substation substation: substationList){
            String stIdSub = substation.getId();
            String bvId = substation.getBvId();
            if (stId.equals(stIdSub)){
                return bvId;
            }
        }
        return null;
    }

    //---------根据各种参数查找各种设备方法---------------------//
    
    
    

    /**
     * 根据输入时间，查找当前时刻的供带关系
     * @param request
     * @param time
     * @return
     */
    @RequestMapping(value="/queryTransformerRelation", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(notes ="查询供带关系", value = "查询供带关系")
    public PowerWebResponse queryTransformerRelation(HttpServletRequest request,
    		String time,  String queryLoad,
    		ServletResponse servletResponse){
        PowerWebResponse response = new PowerWebResponse();
        // 指定允许其他域名访问
        HttpServletResponse responseServ = (HttpServletResponse) servletResponse;
        responseServ.setHeader("Access-Control-Allow-Origin", "*");
        
        // TODO: 如何支持并发查询，使用的是全局变量。
        

        try {
        	
        	// 每次查询前，先将全局变量清空
        	cleanResouce(null);

            if(!StringUtils.isBlank(time)){
                queryMoment = time;
            }
            
            if (null != queryLoad && "1".equals(queryLoad)) {
            	queryLoadForResult = true;
			}else {
				queryLoadForResult = false;
			}
            
            System.out.println("queryMoment="+queryMoment+" queryLoadForResult="+queryLoadForResult);

            long start = System.currentTimeMillis();
            DMUtils dmUtils = new DMUtils();
            Connection connection =dmUtils.getConnection();
            if (null != connection){
                // 创建语句对象,最后关闭,运行游标移动，不能更新数据库，只读
                stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                //数据库语句
                String sql;
//                //结果集，查完一次，关闭一次
//                ResultSet rs;
                //结果集元数据
                ResultSetMetaData rsmd;
                
                //初始化
                DMQuery.queryAllDeviceData(rs, stmt);
                //debug: 查看查询到的厂站中是否有缺失
                System.out.println("查看查询到的厂站中是否有缺失");
                int stationCount = DMQuery.originalSubstationList.size();
                for(int i=0; i<Constants.lackStationName.length; i++) {
                	String lackName = Constants.lackStationName[i];
                	int index = 0;
                    for(Substation substation: DMQuery.originalSubstationList){
                    	String stationName = substation.getName();
                    	if (stationName.contains(lackName) &&
                    			!stationName.contains("老")&&
                    			!stationName.contains("备")&&
                    			!stationName.contains("zfdms")) {
                    		lackStations.add(substation);
							break;
						}
                    	
                    	index++;
                    }
                    
                    if (index == stationCount) {
						System.out.println("查询到的原始数据站中缺失 lackName="+lackName);
						Substation sub = new Substation();
						sub.setName(lackName);
						sub.setId("缺失站ID,随意造的，调试数据");
						lackStations.add(sub);
					}
                }
                
                // 由于End中有使用隔刀数据，其作为过滤源头，应该使用原始数据。
                //查询断路器breaker
                DMQuery.processBreaker(rs, stmt);
                //查询隔刀
                DMQuery.processDisconnector(rs, stmt);
                //查询母线
                DMQuery.processBusbarsection(rs, stmt);
                

                //查询线端表end
                DMQuery.processACLineEnd(rs, stmt);
                //debug:查看End中是否有过滤掉的站名
                System.out.println("查看END是否有缺失");
                int endCount = acLineEndList.size();         
                for(Substation substation: lackStations) {
                	String subStId = substation.getId();
                	String lackName = substation.getName();
                	int index = 0;
                    for(ACLineEnd ae: acLineEndList){
                    	String endStId = ae.getStId();
                    	String endName =ae.getName();
                    	if (endName.contains(lackName)) {
							break;
						}      	
                    	index++;
                    }
                    
                    if (index == endCount) {
						System.out.println("查询到的END中缺失 lackName="+lackName);
					}
                }

                


                //根据bay_id 来查找各个线端状态，过滤源头始祖End
                for(ACLineEnd acLineEnd: acLineEndList){
                    String bayId = acLineEnd.getBayId();
                    String nd = acLineEnd.getNd();
                    String endName = acLineEnd.getName();
                    // TODO:查遥信逻辑需要修改
                    //如果bayId无法找到，需要通过ND继续找到间隔
                    if (null != bayId){
                        //根据bayId找断路器查遥信，如果查不到，则根据ND找隔刀
                        Breaker breaker = findBreakerByBayId(bayId);
                        if (null != breaker){
                            String breakerId = breaker.getId();
                            String breakerName = breaker.getName();
                            boolean result = YXStatusEqualMoment(queryMoment,
                                    breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                            if (result){
                                filterByBreakerEndList.add(acLineEnd);
                            }else {
								System.out.println("查询End遥信状态断开endName="+endName+" breakerName="+breakerName);
							}
                        }else {
                            //bayId不为null，但是找不到断路器， 通过ND找隔刀
                            if ((null!=nd) && !"-1".equals(nd)){
                                Disconnector disconnector = findDisconnectorByNd(nd);
                                if (null != disconnector){       
                                    String disconnetorBayId = disconnector.getBayId();
                                    Breaker breakerByDis = findBreakerByBayId(disconnetorBayId);
                                    if (null != breakerByDis) {
                                        String breakerId = breakerByDis.getId();
                                        String breakerByDisName = breakerByDis.getName();
                                        boolean result = YXStatusEqualMoment(queryMoment,
                                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                                        if (result){
                                            filterByBreakerEndList.add(acLineEnd);
                                        }else {
            								System.out.println("查询End遥信状态断开endName="+endName+" breakerByDisName="+breakerByDisName);
            							}
									}else {
										System.out.println("找到隔刀bayId无法找到断路器，默认闭合 endName="+endName);
										filterByBreakerEndList.add(acLineEnd);
									}
//                                    String disconnetorId = disconnector.getId();
//                                    boolean result = YXStatusEqualMoment(queryMoment,
//                                            disconnetorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
//                                    if (result){
//                                        filterByBreakerEndList.add(acLineEnd);
//                                    }else {
//        								System.out.println("查询End遥信状态断开endName="+endName);
//        							}
                                }else {
                                    logger.error("既找不到断路器，也找不到隔刀, 且bayId 和 nd 皆为有值情况，先默认通的吧！ name="+acLineEnd.getName()
                                            +" stId="+acLineEnd.getStId());
                                    filterByBreakerEndList.add(acLineEnd);
                                }
                            }else {
                                logger.error("既找不到断路器，也找不到隔刀，而且bayId不为空，nd为空，先默认通的吧！ name="+acLineEnd.getName()
                                        +" stId="+acLineEnd.getStId());
                                filterByBreakerEndList.add(acLineEnd);
                            }
                        }
                    }else {
                        //通过ND找隔刀
                        if ((null!=nd) && !"-1".equals(nd)){
                            Disconnector disconnector = findDisconnectorByNd(nd);
                            if (null != disconnector){
                                String disconnetorBayId = disconnector.getBayId();
                                Breaker breakerByDis = findBreakerByBayId(disconnetorBayId);
                                if (null != breakerByDis) {
                                    String breakerId = breakerByDis.getId();
                                    boolean result = YXStatusEqualMoment(queryMoment,
                                            breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                                    if (result){
                                        filterByBreakerEndList.add(acLineEnd);
                                    }else {
        								System.out.println("查询End遥信状态断开endName="+endName);
        							}
								}else {
									System.out.println("找到隔刀bayId无法找到断路器，默认闭合 endName="+endName);
									filterByBreakerEndList.add(acLineEnd);
								}
//                                String disconnetorId = disconnector.getId();
//                                boolean result = YXStatusEqualMoment(queryMoment,
//                                        disconnetorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
//                                if (result){
//                                    filterByBreakerEndList.add(acLineEnd);
//                                }else {
//    								System.out.println("查询End遥信状态断开endName="+endName);
//    							}
                            }else {
                                logger.error("既找不到断路器，也找不到隔刀，bayId为空，ND不为空，先默认通的吧！ name="+acLineEnd.getName()
                                        +" stId="+acLineEnd.getStId());
                                filterByBreakerEndList.add(acLineEnd);
                            }
                        }else {
                            logger.error("既找不到断路器，也找不到隔刀，bayId 和 nd 都为空，丢弃吧！ name="+acLineEnd.getName()
                                    +" stId="+acLineEnd.getStId());
                            // TDDO: DEBUG为了调试查找缺失数据，此处先默认为遥信闭合
                            filterByBreakerEndList.add(acLineEnd);
                        }
                    }
                }

                // 过滤后的线端
                System.out.println("遥信状态为闭acLineEnd个数："+filterByBreakerEndList.size());
                for (ACLineEnd acLineEnd: filterByBreakerEndList){
                    System.out.println("遥信状态为闭，acLineEnd name="+acLineEnd.getName());
                }
                
                
                
                //debug:查看遥信为闭合End中是否有过滤掉的站名
                System.out.println("查看遥信闭合END是否有过滤");
                int filterEndCount = filterByBreakerEndList.size();
                for(int i=0; i<Constants.lackStationName.length; i++) {
                	String lackName = Constants.lackStationName[i];
                	int index = 0;
                    for(ACLineEnd ae: filterByBreakerEndList){
                    	String endName = ae.getName();
                    	if (endName.contains(lackName)) {
							break;
						}
                    	
                    	index++;
                    }
                    
                    if (index == filterEndCount) {
						System.out.println("查看遥信闭合END中缺失 lackName="+lackName);
					}
                }


                //查询线段表
                DMQuery.processACLineSegment(rs, stmt);
               //debug: 将缺失厂站名字转换为stId，进行线段中的ist jst 查找
               for(Substation sub: lackStations) {
               	String id = sub.getId();
               	String stationName = sub.getName();
               	boolean foundSeg = false;
               	for(ACLineSegment as: acLineSegmentList) {
               		String istId = as.getIstId();
               		String jstId = as.getJstId();
               		String segName = as.getName();
               		if (id.equals(istId)||
               				id.equals(jstId)) {
	               			foundSeg = true;
	               			System.out.println("缺失站ID找到线段记录 segName="+segName);
	               			break;
						}
               	}
               	
               	if (!foundSeg) {
               		System.out.println("缺失站ID找不到线段记录！！！ stationName="+stationName);
					}
               	
               }
               
               	//使用线端来过滤线段
                for(ACLineEnd acLineEnd: filterByBreakerEndList){
                    String segIdInEnd = acLineEnd.getAclnsegId();
                    if (null ==segIdInEnd){
                        logger.warn("#####线端表中的segId为null##########");
                        continue;
                    }
                    int segCount = acLineSegmentList.size();
                    int index = 0;
                    for(ACLineSegment acLineSegment: acLineSegmentList){
                        String segmentId = acLineSegment.getId();
                        String segmentName = acLineSegment.getName();
                        if (segmentId.equals(segIdInEnd)){
                            ACLineEndWithSegment acLineEndWithSegment = new ACLineEndWithSegment();
                            acLineEndWithSegment.setId(acLineEnd.getId());
                            acLineEndWithSegment.setName(acLineEnd.getName());
                            acLineEndWithSegment.setStId(acLineEnd.getStId());
                            acLineEndWithSegment.setBayId(acLineEnd.getBayId());
                            acLineEndWithSegment.setBvId(acLineEnd.getBvId());
                            acLineEndWithSegment.setNd(acLineEnd.getNd());
                            acLineEndWithSegment.setAclnsegId(acLineEnd.getAclnsegId());
                            acLineEndWithSegment.setIstId(acLineSegment.getIstId());
                            acLineEndWithSegment.setJstId(acLineSegment.getJstId());
                            acLineEndWithSegment.setInd(acLineSegment.getInd());
                            acLineEndWithSegment.setJnd(acLineSegment.getJnd());
                            acLineEndWithSegment.setAclineId(acLineSegment.getAclineId());
                            acLineEndWithSegment.setSegName(segmentName);
                            acLineEndWithSegmentList.add(acLineEndWithSegment);
                            break;
                        }
                        index++;
                    }
                    if (index == segCount){
                        System.out.println("#####线端表中的segIdInEnd="+segIdInEnd+" 找不到！！！");
                    }
                }

                System.out.println("过滤后的线端和线段结合体acLineEndWithSegment个数："+acLineEndWithSegmentList.size());
                for(ACLineEndWithSegment acLineEndWithSegment: acLineEndWithSegmentList){
                    String segmentId = acLineEndWithSegment.getAclnsegId();
                    String bvId = acLineEndWithSegment.getBvId();
                    String aclineId = acLineEndWithSegment.getAclineId();
                    String segmentName = acLineEndWithSegment.getSegName();
                    System.out.println("#####ACLineEndWithSegment segmentId="+segmentId+" bvId="+bvId
                    +" aclineId="+aclineId+" segmentName="+segmentName);
                }

                //  过滤掉线段两端的end中只有一个遥信闭合的线段
                Iterator segmentIterator = acLineEndWithSegmentList.iterator();
                while (segmentIterator.hasNext()){
                    ACLineEndWithSegment acLineEndWithSegment = (ACLineEndWithSegment)segmentIterator.next();
                    String segId = acLineEndWithSegment.getAclnsegId();
                    List<ACLineEnd> acLineEnds = new ArrayList<>();
                    //根据线段Id查询到两个或者多个线端，如果线端中有遥信为开的，过滤此线段
                    for(ACLineEnd acLineEnd: acLineEndList){
                        String aclnsegId = acLineEnd.getAclnsegId();
                        if (segId.equals(aclnsegId)){
                            acLineEnds.add(acLineEnd);
                        }
                    }

                    for(ACLineEnd acLineEnd: acLineEnds){
                        if (!filterByBreakerEndList.contains(acLineEnd)){
                            String name = acLineEnd.getName();
                            System.out.println("过滤掉的线段两端只有一个end为闭合的情况 end name="+name);
                            segmentIterator.remove();
                            break;
                        }
                    }
                }
                System.out.println("过滤掉线段两端只有一个End闭合情况后acLineEndWithSegment个数："+acLineEndWithSegmentList.size());

                //debug:查看遥信为闭合End中是否有过滤掉的站名
                System.out.println("查看过滤线段之后是否有缺失");
                int filterSegCount = acLineEndWithSegmentList.size();
                for(int i=0; i<Constants.lackStationName.length; i++) {
                	String lackName = Constants.lackStationName[i];
                	int index = 0;
                    for(ACLineEndWithSegment ae: acLineEndWithSegmentList){
                    	String endName = ae.getName();
                    	if (endName.contains(lackName)) {
							break;
						}
                    	
                    	index++;
                    }
                    
                    if (index == filterSegCount) {
						System.out.println("过滤线段之后缺失 lackName="+lackName);
					}
                }

                

                //查询厂站表
                DMQuery.processSubstation(rs, stmt);
                //debug:调试查看过滤后的厂站中缺失哪些站
                int subStationCount = substationList.size();
                for(Substation substation: lackStations) {
                	String lackName = substation.getName();
                	String stId = substation.getId();
                	int index = 0;
                    for(Substation sub: substationList){
                    	String stationName = sub.getName();
                    	if (stationName.contains(lackName)) {
							break;
						} 	
                    	index++;
                    }
                    
                    if (index == subStationCount) {
						System.out.println("查询到的过滤后的厂站中缺失 lackName="+lackName);
					}
                }
                
 


                //根据厂站构建变电站类
                for(Substation substation: substationList){
                    String stId = substation.getId();
                    String name = substation.getName();
                    String bvId = substation.getBvId();
                    List<ACLineEndWithSegment> endWithSegments = new ArrayList<>();
                    for (ACLineEndWithSegment acLineEndWithSegment: acLineEndWithSegmentList){
                        String stIdInSeg = acLineEndWithSegment.getStId();
                        if (stId.equals(stIdInSeg)){
                            endWithSegments.add(acLineEndWithSegment);
                        }
                    }

                    TransformerSubstation transformerSubstation = new TransformerSubstation();
                    transformerSubstation.setStId(stId);
                    transformerSubstation.setName(name);
                    transformerSubstation.setBvId(bvId);
                    transformerSubstation.setEndWithSegments(endWithSegments);
                    transformerSubstationList.add(transformerSubstation);
                }

                System.out.println("构建变电站transformerSubstation个数："+transformerSubstationList.size());
                for(TransformerSubstation transformerSubstation: transformerSubstationList){
                	String stId = transformerSubstation.getStId();
                	String subName = transformerSubstation.getName();
                	List<ACLineEndWithSegment> endWithSegments = transformerSubstation.getEndWithSegments();
                	for(ACLineEndWithSegment endWithSegment: endWithSegments) {
                		String segName = endWithSegment.getSegName();
                		System.out.println("厂站中添加了遥信为闭合的线段 站id="+stId+" name="+subName
                				+" segName="+segName);
                	}
                }

                //构建T接线数据，以acline_id作为key来构建，一个acline_id数据对应多个厂站信息集合
                for(int i=0; i<acLineEndWithSegmentList.size(); i++){
                    String aclineId = acLineEndWithSegmentList.get(i).getAclineId();
                    //过滤无用数据
                    if((null == aclineId) || "0".equals(aclineId)){
                        continue;
                    }
                    //已经添加过到列表中的不需要再查询处理
                    boolean hasFound = false;
                    for(ACLineWithSubstation acLineWithSubstation: acLineWithSubstationList){
                        String aclineIdInList = acLineWithSubstation.getAclineId();
                        if (aclineId.equals(aclineIdInList)){
                            hasFound = true;
                            break;
                        }
                    }
                    if (hasFound){
                        continue;
                    }

                    String stId = acLineEndWithSegmentList.get(i).getStId();
                    ACLineEndWithSegment acLineEndWithSegment = acLineEndWithSegmentList.get(i);
                    ACLineWithSubstation acLineWithSubstation = new ACLineWithSubstation();
                    acLineWithSubstation.setAclineId(aclineId);

                    List<SubstationWithSegment> substationWithSegments = new ArrayList<>();
                    SubstationWithSegment substationWithSegment = new SubstationWithSegment();
                    substationWithSegment.setId(stId);
                    substationWithSegment.setAcLineEndWithSegment(acLineEndWithSegment);
                    substationWithSegments.add(substationWithSegment);
                    for(int j=i+1; j<acLineEndWithSegmentList.size(); j++){
                        String aclineIdNext = acLineEndWithSegmentList.get(j).getAclineId();
                        //过滤无用数据
                        if((null == aclineIdNext) || "0".equals(aclineIdNext)){
                            continue;
                        }
                        if (aclineIdNext.equals(aclineId)){
                            String stIdNext = acLineEndWithSegmentList.get(j).getStId();
                            ACLineEndWithSegment acLineEndWithSegmentNext = acLineEndWithSegmentList.get(j);
                            SubstationWithSegment substationWithSegmentNext = new SubstationWithSegment();
                            substationWithSegmentNext.setId(stIdNext);
                            substationWithSegmentNext.setAcLineEndWithSegment(acLineEndWithSegmentNext);
                            substationWithSegments.add(substationWithSegmentNext);
                        }
                    }
                    acLineWithSubstation.setSubstationWithSegments(substationWithSegments);
                    acLineWithSubstationList.add(acLineWithSubstation);
                }

                System.out.println("构建出的带有acline_id的情况个数："+acLineWithSubstationList.size());
                int indexT = 0;
                for (ACLineWithSubstation acLineWithSubstation: acLineWithSubstationList){
                    String aclineId = acLineWithSubstation.getAclineId();
                    List<SubstationWithSegment> substationWithSegments = acLineWithSubstation.getSubstationWithSegments();
                    if (null != substationWithSegments && substationWithSegments.size() > 1){
                        indexT ++;
                        System.out.println("T接线 acline_id ："+aclineId);
                        for(SubstationWithSegment substationWithSegment: substationWithSegments){
                            System.out.println("stId ："+substationWithSegment.getId());
                        }
                    }
                }
                System.out.println("构建出的带有T接线的情况个数："+indexT);
                //移除acline_id个数小于等于1的情况
                Iterator iteratorT = acLineWithSubstationList.iterator();
                while (iteratorT.hasNext()){
                    ACLineWithSubstation acLineWithSubstation = (ACLineWithSubstation)iteratorT.next();
                    List<SubstationWithSegment> substationWithSegments = acLineWithSubstation.getSubstationWithSegments();
                    int count = substationWithSegments==null ? 0:substationWithSegments.size();
                    if (count <= 1){
                        iteratorT.remove();
                    }
                }
                System.out.println("acLineWithSubstationList个数："+acLineWithSubstationList.size());


                //查询主变
                DMQuery.processPowerTransformer(rs, stmt);
                //debug: 查看查询到的主变中是否有缺失
                System.out.println("查看查询到的主变中是否有缺失");
                int transformerCount = TransformerSubstationConntroller.powerTransformerList.size();
                for(int i=0; i<Constants.lackStationName.length; i++) {
                	String lackName = Constants.lackStationName[i];
                	int index = 0;
                    for(PowerTransformer powerTransformer: TransformerSubstationConntroller.powerTransformerList){
                    	String PowerName = powerTransformer.getName();
                    	if (PowerName.contains(lackName)) {
							break;
						}
                    	
                    	index++;
                    }
                    
                    if (index == transformerCount) {
						System.out.println("查询到的主变中缺失 lackName="+lackName);
					}
                }
                
                //查询绕阻
                DMQuery.processTransformerwinding(rs, stmt);

                //将主变遥信状态查找出来
                putYXStatusToTransformerWinding(rs, stmt);                      
                //添加在运行的主变到变电站类中
                addRunningPowerTransformerToSubstation();
                //显示运行时主变信息
                System.out.println("##############运行时主变####################");
                for(TransformerSubstation transformerSubstation : transformerSubstationList){
                    List<PowerTransformerWithWinding> running = transformerSubstation.getRunningTransformers();
                    for(PowerTransformerWithWinding powerTransformerWithWinding: running){
                        String windingName = powerTransformerWithWinding.getName();
                        System.out.println(windingName);
                    }
                }               

                //去除没有运行时主变的变电站
                Iterator iterator = transformerSubstationList.iterator();
                while (iterator.hasNext()){
                    TransformerSubstation transformerSubstation = (TransformerSubstation)iterator.next();
                    int running = transformerSubstation.getRunningTransformers()==null?0:transformerSubstation.getRunningTransformers().size();
                    if (running == 0){
                        iterator.remove();
                    }
                }
                
                //debug: 查看查询到的运行时主变中是否有缺失
                System.out.println("查看查询到的运行时主变中的站是否有缺失");
                int substationCount = TransformerSubstationConntroller.transformerSubstationList.size();
                for(int i=0; i<Constants.lackStationName.length; i++) {
                	String lackName = Constants.lackStationName[i];
                	int index = 0;
                    for(TransformerSubstation substation: TransformerSubstationConntroller.transformerSubstationList){
                    	String substationName = substation.getName();
                    	if (substationName.contains(lackName)) {
							break;
						}
                    	
                    	index++;
                    }
                    
                    if (index == substationCount) {
						System.out.println("查询到的运行时主变的站缺失 lackName="+lackName);
					}
                }

                //构建父子拓扑
                ArrayList<TransformerSubstation> node220 = new ArrayList<>();
                ArrayList<TransformerSubstation> node110 = new ArrayList<>();
                ArrayList<TransformerSubstation> node35 = new ArrayList<>();
                ArrayList<TransformerSubstation> node10 = new ArrayList<>();
                for(TransformerSubstation transformerSubstation: transformerSubstationList){
                    String bvId = transformerSubstation.getBvId();
                    if (Constants.BV_ID_220.equals(bvId)){
                        node220.add(transformerSubstation);
                    }else if(Constants.BV_ID_110.equals(bvId)){
                        node110.add(transformerSubstation);
                    }else if(Constants.BV_ID_35.equals(bvId)){
                        node35.add(transformerSubstation);
                    }else if(Constants.BV_ID_10.equals(bvId)){
                        node10.add(transformerSubstation);
                    }
                }
                genTree(node220, node110);
                genTree(node110, node35);
                genTree(node35, node10);
                // TODO: 拓扑关系还需要考虑T接线，还有同级直接引出的可能在线段表中没有描述进出线关系的
                genTreeForTNode();
                
                // TODO: 构建电压等级相等的站的拓扑，需要找母线的找母线
                genSameBvIdForIstAndJstId(Constants.BV_ID_110);
                genSameBvIdForIstAndJstId(Constants.BV_ID_35);
                genTreeForTNodeSameBvId();
                
                //去除非220站数据，便于分析数据
                //debugForRemoveStationNot220();
                //debug: 查看构建完拓扑父子关系之后缺少的名字
//                System.out.println("查看构建完拓扑父子关系是否有缺失");
//                int treeSubstationCount = TransformerSubstationConntroller.transformerSubstationList.size();
//                for(int i=0; i<Constants.lackStationName.length; i++) {
//                	String lackName = Constants.lackStationName[i];
//                	int index = 0;
//                    for(TransformerSubstation substation: TransformerSubstationConntroller.transformerSubstationList){
//                    	boolean found = scanAllTreeNode(lackName, substation);
//                    	if (found) {
//							break;
//						}
//                    	
//                    	index++;
//                    }
//                    
//                    if (index == treeSubstationCount) {
//						System.out.println("查看构建完拓扑父子关系缺失 lackName="+lackName);
//					}else {
//						System.out.println("查看构建完拓扑父子关系找到 lackName="+lackName);
//					}
//                }
//                collectAllNodeAfter220Filter(transformerSubstationList);
//                int collectCount = allNode220Filter.size();
//                System.out.println("allNode220Filter size="+collectCount);
//	              for(Substation substation: lackStations) {
//	            	String lackName = substation.getName();
//	            	String subId = substation.getId();
//	            	int index=0;
//	                for(TransformerSubstation transformerSubstation: allNode220Filter) {
//	                	String transformerSubId = transformerSubstation.getStId();
//	                	if (transformerSubId.equals(subId)) {
//							break;
//						}
//	                	index++;
//	                }
//	                
//	                if (index == collectCount) {
//	                	System.out.println("查看构建完拓扑父子关系缺失 lackName="+lackName);
//					}
//	              }
//
//                
//                
//                int nodeInTreeCount = NodeInTree.size();
//                System.out.println("NodeInTree size="+nodeInTreeCount);
//                for(Substation substation: lackStations) {
//                	String subId = substation.getId();
//                	String subName = substation.getName();
//                	int index = 0;
//                	for(TransformerSubstation transformerSubstation: NodeInTree) {
//                		String transformerSubId = transformerSubstation.getStId();
//                		if (subId.equals(transformerSubId)) {
//                			System.out.println("缺失站在拓扑树种找到 subName="+subName);
//                			break;
//						}
//                		
//                		index++;
//                	}
//                	
//                	if (index == nodeInTreeCount) {
//                		System.out.println("缺失站在拓扑树种没找到 subName="+subName);
//					}
//                }
//               
//                // NODEINTREE 中可能存在没有从220 开始的站，后面被过滤掉了
//                int filterCount = allNode220Filter.size();
//                for(TransformerSubstation transformerSubstation: NodeInTree) {
//                	int index = 0;
//                	String stIdInTree = transformerSubstation.getStId();
//                	String nodeTreeName = transformerSubstation.getName();
//                	String bvId = transformerSubstation.getBvId();
//                	for(TransformerSubstation filterSub: allNode220Filter) {
//                		String stIdFilter = filterSub.getStId();
//                		if (stIdInTree.equals(stIdFilter)) {
//							break;
//						}
//                		index++;
//                	}
//                	
//                	if (index == filterCount) {
//						System.out.println("没有找到220的父站但是在树中有过的站 nodeTreeName="+nodeTreeName
//								+" bvId="+bvId);
//					}
//                	
//                }
                

                //  在主变各侧加入和母线的关联关系
                processPowerWindingWithBusRelation();
                

//              response.setData(transformerSubstationList);


                // 处理进出线和母线关系：
                // 在拓扑中，逐个遍历各个供主变输出侧的母线与出线的关系
                // 以及进线和该带主变输入侧的母线， 注意线变组的处理
                processInOutLinesWithBusRelation();
                
                

//                // TODO:遍历拓扑，找出站间供带关系
//                for(PowerSupplyData powerSupplyData: powerSupplyData){
//                    String stationName = powerSupplyData.getStationName();
//                    String powerNumber = powerSupplyData.getPowerNumber();
//                    String voltage = powerSupplyData.getVoltage();
//                    System.out.println(stationName+" "+powerNumber+" "+voltage);
//                }

                //找出进出线和主变的关系
                inOutLinesToPower();
                

                // TODO:将构建电压等级相等的情况放置在常规和T电压不等情况之后，这样后面找主变母线进出线的逻辑中
                // 进出线的条数又有着落了
                // TODO: 处理非T接线但是电压等级相同的高压侧输出线，例如靖港 郭亮 黑麋峰
//                genSameBvIdForIstAndJstId(Constants.BV_ID_110);
//                genSameBvIdForIstAndJstId(Constants.BV_ID_35);
                // TODO：郭亮 靖港 这种先构建ist jst互为各自的，后面的郭亮、宝雍这种T就会有进线了
                //genTreeForTNodeSameBvId();
                
                
//                genSameBvIdForIstAndJstId(Constants.BV_ID_110);
//                genSameBvIdForIstAndJstId(Constants.BV_ID_35);      
//                // TODO： 处理电压等级相等的T, 放在非T 电压等级处理之前，避免进出线中判断虚拟线逻辑
//                // TODO：郭亮 靖港 这种先构建ist jst互为各自的，后面的郭亮、宝雍这种T就会有进线了
//                genTreeForTNodeSameBvId();

                
                debugForRemoveStationNot220();
                System.out.println("查看构建电压相等情况后是否有缺失");
                collectAllNodeAfter220Filter(transformerSubstationList);
                //220站从上往下的拓扑
                int collectCount = allNode220Filter.size();
                System.out.println("allNode220Filter size="+collectCount);
	              for(Substation substation: lackStations) {
	            	String lackName = substation.getName();
	            	String subId = substation.getId();
	            	int index=0;
	                for(TransformerSubstation transformerSubstation: allNode220Filter) {
	                	String transformerSubId = transformerSubstation.getStId();
	                	if (transformerSubId.equals(subId)) {
							break;
						}
	                	index++;
	                }
	                
	                if (index == collectCount) {
	                	System.out.println("查看构建完拓扑父子关系缺失 lackName="+lackName);
					}
	              }

                
	            //遍历过的所有站
                int nodeInTreeCount = NodeInTree.size();
                System.out.println("NodeInTree size="+nodeInTreeCount);
                for(Substation substation: lackStations) {
                	String subId = substation.getId();
                	String subName = substation.getName();
                	int index = 0;
                	for(TransformerSubstation transformerSubstation: NodeInTree) {
                		String transformerSubId = transformerSubstation.getStId();
                		if (subId.equals(transformerSubId)) {
                			System.out.println("缺失站在遍历树的过程中找到 subName="+subName);
                			break;
						}
                		
                		index++;
                	}
                	
                	if (index == nodeInTreeCount) {
                		System.out.println("缺失站在遍历树的过程中没找到 subName="+subName);
					}
                }
               
                // NODEINTREE 中可能存在没有从220 开始的站，后面被过滤掉了
                int filterCount = allNode220Filter.size();
                for(TransformerSubstation transformerSubstation: NodeInTree) {
                	int index = 0;
                	String stIdInTree = transformerSubstation.getStId();
                	String nodeTreeName = transformerSubstation.getName();
                	String bvId = transformerSubstation.getBvId();
                	for(TransformerSubstation filterSub: allNode220Filter) {
                		String stIdFilter = filterSub.getStId();
                		if (stIdInTree.equals(stIdFilter)) {
							break;
						}
                		index++;
                	}
                	
                	if (index == filterCount) {
						System.out.println("没有找到220的父站但是在树中有过的站 nodeTreeName="+nodeTreeName
								+" bvId="+bvId);
					}
                	
                }
                //显示非220站没有进线，以及220站没有出线
                debugShow220StationInfo();
  
                
                //显示供带关系
                String tomcatPath = request.getSession().getServletContext().getRealPath("/"); 
                String filePath = displayPowerSupplyRelation(tomcatPath);
                response.setData(filePath);
                
                // TODO:DEBUG 调试数据不全, 供带中有哪些缺失站
                int supplyCount = supplyBeltStationList.size();
                System.out.println("supplyBeltStationList size="+supplyCount);
	              for(Substation substation: lackStations) {
	            	String lackName = substation.getName();
	            	String subId = substation.getId();
	            	int index=0;
	                for(TransformerSubstation transformerSubstation: supplyBeltStationList) {
	                	String transformerSubId = transformerSubstation.getStId();
	                	if (transformerSubId.equals(subId)) {
							break;
						}
	                	index++;
	                }
	                
	                if (index == supplyCount) {
	                	System.out.println("供带关系遍历站中缺失 lackName="+lackName);
					}
	              }
	              
	              
	                int showsupplyCount = showSupplyStationList.size();
	                System.out.println("showSupplyStationList size="+showsupplyCount);
		              for(Substation substation: lackStations) {
		            	String lackName = substation.getName();
		            	String bvId= substation.getBvId();
		            	String subId = substation.getId();
		            	int index=0;
		                for(TransformerSubstation transformerSubstation: showSupplyStationList) {
		                	String transformerSubId = transformerSubstation.getStId();
		                	if (transformerSubId.equals(subId)) {
								break;
							}
		                	index++;
		                }
		                
		                if (index == showsupplyCount) {
		                	System.out.println("供带关系有数据展示站中缺失 lackName="+lackName+" bvId="+bvId);
						}
		              }
		              
//		              System.out.println("#####################");
//		              for(Substation substation: lackStations) {
//		            	  String lackName = substation.getName();
//		            	  String subId = substation.getId();
//		            	  System.out.println("lackName="+lackName+" subId="+subId);
//		              }


//                //去除非220站数据，便于分析数据
//                debugForRemoveStationNot220();
//                response.setData(transformerSubstationList);


                long end = System.currentTimeMillis();
                System.out.println("总共花费时间："+(end-start)/1000+" 秒");


            }else {
                System.out.println("获取连接为空");
                response.setFailure("操作失败");
            }

//            // TODO； 将所有全局变量清空，以及关闭数据库连接；
            cleanResouce(connection);

        } catch (Exception e) {
            response.setFailure("操作失败");
            e.printStackTrace();
            cleanResouce(null);
            // TODO:访问数据库网络连接失败，如何清理数据，返回异常
        }
        return response;
    }
    
    
    
    //调试方法，显示220拓扑站中的进出线以及主变数据
    private void debugShow220StationInfo() {
    	//显示非220站没有进线，以及220站没有出线
        for(TransformerSubstation transformerSubstation: allNode220Filter) {
        	String bvId = transformerSubstation.getBvId();
        	List<InOutLines> inlines = transformerSubstation.getInLines();
        	List<InOutLines> outlines = transformerSubstation.getOutLines();
        	List<TransformerWindingRelateBus> transformerWindingRelateBuses = transformerSubstation.getTransformerWindingRelateBuses();
        	List<TransformerSubstation> children = transformerSubstation.getChildren();
        	
        	int windingCount = (transformerWindingRelateBuses==null)?0:transformerWindingRelateBuses.size();
        	int inlineCount = (inlines == null)?0:inlines.size();
        	int outlineCount = (outlines == null)?0:outlines.size();
        	String stationName = transformerSubstation.getName();
        	System.out.println("变电站进出线条数调试， stationName="+stationName+" windingCount="+windingCount);

        	
        	if (Constants.BV_ID_220.equals(bvId)) {
				if (outlineCount == 0) {
					System.out.println("变电站进出线条数，220站无出线 stationName="+stationName);
				}
				
            	for(InOutLines outline: outlines) {
            		List<TransformerWinding> transformerWindings = outline.getTransformerWindings();
            		String parentStId = outline.getParentStId();
            		int windingsCount = (transformerWindings==null)?0:transformerWindings.size();
            		if (windingsCount == 0) {
            			System.out.println("出线连接主变为0 parentStId="+parentStId+" stationName="+stationName);
					}
            	}
            	
			}else {
				if (inlineCount == 0) {
					System.out.println("变电站进出线条数，非220站无进线 stationName="+stationName);
				}
				
            	for(InOutLines inline: inlines) {
            		List<TransformerWinding> transformerWindings = inline.getTransformerWindings();
            		String sonStId = inline.getSonStId();
            		int windingsCount = (transformerWindings==null)?0:transformerWindings.size();
            		if (windingsCount == 0) {
            			System.out.println("进线连接主变为0 sonStId="+sonStId+" stationName="+stationName);
					}
            	}
			}
        	
        	if (windingCount==0) {
        		System.out.println("变电站无主变 stationName="+stationName+" bvId="+bvId);
			}
        	
        	//显示所有的父子关系
        	if (null != children && children.size()>0) {
				for(TransformerSubstation child: children) {
					String childName = child.getName();
					List<InOutLines> childinlines = child.getInLines();
					int childinlineCount = (childinlines==null)?0:childinlines.size();
					System.out.println("父站stationName="+stationName+" 父站出线条数："+outlineCount
							+" 子站childName="+childName+" 子站进线条数："+childinlineCount);
				}
			}
        	
        }
    }
    
    //调试方法，遍历父子站名
    private boolean scanAllTreeNode(String lackName, TransformerSubstation substation) {
    	
    	String substationName = substation.getName();
    	if (substationName.contains(lackName)) {
    		return true;
		}
    	
    	List<TransformerSubstation> children = substation.getChildren();
    	if (null != children && children.size()>0) {
			for(TransformerSubstation child: children) {
//				String childName = child.getName();
//				if (childName.equals(lackName)) {
//					return true;
//				}
				boolean result =  scanAllTreeNode(lackName, child);
				return result;
			}
		}
        
        return false;
	}
    
    //递归找出过滤后只剩下220的所有站
    List<TransformerSubstation> allNode220Filter = new ArrayList<TransformerSubstation>();
    private void collectAllNodeAfter220Filter(List<TransformerSubstation> substations) {
    	
    	for(TransformerSubstation transformerSubstation: substations) {
    	
    		if (!allNode220Filter.contains(transformerSubstation)) {
    			allNode220Filter.add(transformerSubstation);
			}
    		
        	List<TransformerSubstation> children = transformerSubstation.getChildren();
        	if (null != children && children.size()>0) {
        		collectAllNodeAfter220Filter(children);
        	}
    	}
	}



    //去除非220kv站数据，调试使用，便于分析数据
    private void debugForRemoveStationNot220(){
        //形成拓扑后去除非220的站，因为220中的子节点已经包含了110 35  10, 暂时先不去除，可能需要找出线使用
        Iterator transformerSubstationIterator = transformerSubstationList.iterator();
        while (transformerSubstationIterator.hasNext()){
            TransformerSubstation transformerSubstation = (TransformerSubstation)transformerSubstationIterator.next();
            String bvId = transformerSubstation.getBvId();
            if (!Constants.BV_ID_220.equals(bvId)){
                transformerSubstationIterator.remove();
            }
        }
    }

    /**
     *   清理全局变量和资源
     * @param connection
     */
    private void cleanResouce(Connection connection){
    	
    	//记录防重复数据
    	hasFoundSupplyData.clear();
    	//用户站数据
    	DMQuery.userSubstationId.clear();
    	//清空遥测数据
        queryedWindingYC.clear();
    	
    	//调试数据清理
    	supplyBeltStationList.clear();
    	lackStations.clear();
    	
    	//增加清理原始静态数据
    	DMQuery.originalBreakerList.clear();
    	DMQuery.originalAcLineEndList.clear();
    	DMQuery.originalAcLineSegmentList.clear();
    	DMQuery.originalSubstationList.clear();
    	DMQuery.originalPowerTransformerList.clear();
    	DMQuery.originalTransformerWindingList.clear();
    	DMQuery.originalDisconnectorList.clear();
    	DMQuery.originalBusbarsectionList.clear();
    	
        breakerList.clear();
        //线段列表
        acLineSegmentList.clear();
        // 由端同间隔的断路器状态过滤后的线端设备
        filterByBreakerEndList.clear();
        // 根据线端和线段一起确定的数据结构
        acLineEndWithSegmentList.clear();
        //过滤后的厂站列表
        substationList.clear();
        //变电站类列表
        transformerSubstationList.clear();
        //过滤后的主变列表
        powerTransformerList.clear();
        //过滤后的绕阻列表
        transformerWindingList.clear();
        //带有绕阻信息的主变
        powerTransformerWithWindingList.clear();
        //隔刀列表
        disconnectorList.clear();
        //母线列表
        busbarsectionList.clear();
        //T接线列表
        acLineWithSubstationList.clear();
        //记录最终供带关系列表
        powerSupplyData.clear();
        //原始线端数据，没有过滤的
        acLineEndList.clear();
        //变成全局对象
        if (null != stmt){
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if(null != connection){
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
    
    //当只有一条进线或者出现时处理和主变关系
    private void processOneLineWithPower(List<TransformerWindingRelateBus> transformerWindingRelateBuses,
    		InOutLines line, boolean isInLine, String bvIdSub, List<TransformerWinding> transformerWindings) {
    	System.out.println("processOneLineWithPower isInLine="+isInLine);
    	if (!isInLine) {
    		String outlineBvId = line.getBvId();
        	if(Constants.BV_ID_220.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
                //多个主变和出线的连接
                for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses){
                    //判断是否中低压输出侧
                    String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                    String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                    String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                    String powerName = transformerWindingRelateBus.getTransformerWinding().getName();

                    //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
                    if(!outlineBvId.equals(bvIdWinding)){
                        System.out.println("#####出现的电压等级和主变不一致！！！！powerName="+powerName
                        		+" outlineBvId="+outlineBvId+" bvIdWinding="+bvIdWinding);
                        continue;
                    }
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //中低压侧且为110或者35KV的，因为还会出现6KV这种
                        if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding) || Constants.BV_ID_35.equals(bvIdWinding))) {
                            TransformerWinding transformerWinding = transformerWindingRelateBus.getTransformerWinding();
                            transformerWindings.add(transformerWinding);
                            
                            System.out.println("processOneLineWithPower添加主变到出线 windingName="+transformerWinding.getName());
                        }
                    }
                }
                
                //设置出线带的主变记录
                line.setTransformerWindings(transformerWindings);
        	}

		}else {
			//进线
			if(Constants.BV_ID_35.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
                //多个主变和进线的连接
                for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses){
                    //判断是否中低压输出侧
                    String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                    String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                    String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                    String powerName = transformerWindingRelateBus.getTransformerWinding().getName();

//                //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
//                if(!bvIdOutLine.equals(bvIdWinding)){
//                    System.out.println("#####出现的电压等级和主变不一致！！！！powerName="+powerName);
//                    continue;
//                }
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //中低压侧且为110或者35KV的，因为还会出现6KV这种
                        if ((Constants.WINDING_HIGH.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                            TransformerWinding transformerWinding = transformerWindingRelateBus.getTransformerWinding();
                            transformerWindings.add(transformerWinding);
                            System.out.println("processOneLineWithPower添加主变到进线 windingName="+transformerWinding.getName());
                        }
                    }
                }
                
                line.setTransformerWindings(transformerWindings);
			}
		}
    }

    //处理站内关系，找出所有进出线连接的主变
    private void inOutLinesToPower(){
        for (TransformerSubstation transformerSubstation : transformerSubstationList) {
            String bvIdSub = transformerSubstation.getBvId();
            String stationName = transformerSubstation.getName();
            //需要遍历的线为非虚拟线,使用一条线逻辑判断又需要用总条数来计算
            //总出线集合
            List<InOutLines> outLines = transformerSubstation.getOutLines();
            //总进线集合
            List<InOutLines> inLines = transformerSubstation.getInLines();
            int outLineCount = (outLines==null) ? 0: outLines.size();
            int inLineCount = (inLines==null) ? 0: inLines.size();
            //去除虚拟出线集合
            List<InOutLines> outLinesWithoutVirtual = transformerSubstation.getOutLinesWitoutVirtual();
            //去除虚拟进线集合
            List<InOutLines> inLinesWithoutVirtual = transformerSubstation.getInLinesWitoutVirtual();
            // 计算进出线条数的时候要算上虚拟线，否则一条情况为误判
            int outLineWithoutVirtualCount = (outLinesWithoutVirtual==null) ? 0: outLinesWithoutVirtual.size();
            int inLineWithoutVirtualCount = (inLinesWithoutVirtual==null) ? 0: inLinesWithoutVirtual.size();

            
            //主变母线集合
            List<TransformerWindingRelateBus> transformerWindingRelateBuses = transformerSubstation.getTransformerWindingRelateBuses();
            //运行时主变集合
            List<PowerTransformerWithWinding> runningTransformers = transformerSubstation.getRunningTransformers();
            int runningCount = (null==runningTransformers)?0:runningTransformers.size();
            System.out.println("inOutLinesToPower stationName"+stationName+" 运行时主变个数 runningCount="+runningCount
            		+" 出线条数="+outLineCount+" 进线条数="+inLineCount+" bvIdSub="+bvIdSub);


            // 进出线和站之间，先确认站中的运行主变个数，如果只有一个，则必定和进出线连接。
            //输出侧的母线与出线关系, 220和110站输出，都还有要关注的35KV可能
            if(Constants.BV_ID_220.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
            	//先处理完一条线的情况，总条数为1，不要处理虚拟线
            	if (outLineCount == 1 && outLineWithoutVirtualCount == 1) {
            		InOutLines oneOutLine = outLinesWithoutVirtual.get(0);
                    List<TransformerWinding> transformerWindingsOneLine = oneOutLine.getTransformerWindings();
                    if (null == transformerWindingsOneLine){
                    	transformerWindingsOneLine = new ArrayList<>();
                    }
            		processOneLineWithPower(transformerWindingRelateBuses, oneOutLine,
            				false, bvIdSub, transformerWindingsOneLine);
				}else if (outLineCount > 1) {
					//多条出线情况，不要处理虚拟线
                    for(InOutLines outLine: outLinesWithoutVirtual){
                        String bvIdOutLine = outLine.getBvId();
                        //进出线中已经连接的主变数据
                        List<TransformerWinding> transformerWindings = outLine.getTransformerWindings();
                        if (null == transformerWindings){
                            transformerWindings = new ArrayList<>();
                        }
                        //一个主变和出现的连接
                        if (runningCount == 1){
                            List<TransformerWinding> onePower = transformerSubstation.getRunningTransformers().get(0).getWindings();
                            for(TransformerWinding transformerWinding: onePower){
                                String windType = transformerWinding.getWindType();
                                String yxClose = transformerWinding.getYx_close();
                                String bvId = transformerWinding.getBvId();

                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvId) || Constants.BV_ID_35.equals(bvId))) {

                                        //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
                                        if(!bvIdOutLine.equals(bvId)){
                                            System.out.println("只有一个主变, 出线电压侧和主变电压侧不等！！！");
                                            continue;
                                        }
                                        transformerWindings.add(transformerWinding);
                                        System.out.println("只有一个主变，找到出线连接的主变侧");
                                    }
                                }
                            }
                        }else {
                            //多个主变和出线的连接
                            for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses){
                                //判断是否中低压输出侧
                                String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                                String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                                String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                                String powerName = transformerWindingRelateBus.getTransformerWinding().getName();

                                //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
                                if(!bvIdOutLine.equals(bvIdWinding)){
                                    System.out.println("#####出现的电压等级和主变不一致！！！！powerName="+powerName
                                    		+" bvIdOutLine="+bvIdOutLine+" bvIdWinding="+bvIdWinding);
                                    continue;
                                }
                                //遥信闭合的
                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvIdWinding) || Constants.BV_ID_35.equals(bvIdWinding))) {
                                        boolean result = isOutOrInLineConnectWinding(outLine, transformerWindingRelateBus, false);
                                        TransformerWinding transformerWinding = transformerWindingRelateBus.getTransformerWinding();
                                        if (result){
                                            transformerWindings.add(transformerWinding);
                                        }
                                    }
                                }
                            }
                        }
                        //设置出线带的主变记录
                        outLine.setTransformerWindings(transformerWindings);
                        
                        // TODO:DEBUG 调试数据不全
                        List<TransformerWinding> windings = outLine.getTransformerWindings();
                        int windingCount = (windings==null)?0:windings.size();
                        if (windingCount == 0) {
							System.out.println("inOutLinesToPower出线没有连接一个主变 stationName="+stationName);
							// TODO:DEBUG， 无奈加上一个主变吧
							List<TransformerWinding> onePower = transformerSubstation.getRunningTransformers().get(0).getWindings();
                            for(TransformerWinding transformerWinding: onePower){
                                String windType = transformerWinding.getWindType();
                                String yxClose = transformerWinding.getYx_close();
                                String bvId = transformerWinding.getBvId();
                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvId) || Constants.BV_ID_35.equals(bvId))) {
                                    	transformerWindings.add(transformerWinding);
                                    	outLine.setTransformerWindings(transformerWindings);
                                    	System.out.println("没有连接一个主变, 手动添加主变成功 windingName="+transformerWinding.getName());
                                    }
                                }
                            }
						}
                    }
				}
            }

            //进线与输入侧的母线关系
            if(Constants.BV_ID_35.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
            	
            	if (inLineCount == 1 && inLineWithoutVirtualCount == 1) {
            		InOutLines oneInLine = inLines.get(0);
                    List<TransformerWinding> transformerWindingsOneLine = oneInLine.getTransformerWindings();
                    if (null == transformerWindingsOneLine){
                    	transformerWindingsOneLine = new ArrayList<>();
                    }
            		processOneLineWithPower(transformerWindingRelateBuses, oneInLine,
            				true, bvIdSub, transformerWindingsOneLine);
				}else if (inLineCount>1) {
					//多条进线，不处理虚拟线
                    for(InOutLines inline: inLinesWithoutVirtual){
                        String bvIdOutLine = inline.getBvId();
                        //进出线中已经连接的主变数据
                        List<TransformerWinding> transformerWindings = inline.getTransformerWindings();
                        if (null == transformerWindings){
                            transformerWindings = new ArrayList<>();
                        }

//                        for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses){
//                            //判断是否中低压输出侧
//                            String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
//                            String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
//                            String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
//                            String powerName = transformerWindingRelateBus.getTransformerWinding().getName();
//
////                        //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
////                        if(!bvIdOutLine.equals(bvIdWinding)){
////                            System.out.println("#####出现的电压等级和主变不一致！！！！powerName="+powerName);
////                            continue;
////                        }
//                            //遥信闭合的
//                            if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
//                                //中低压侧且为110或者35KV的，因为还会出现6KV这种
//                                if ((Constants.WINDING_HIGH.equals(windType)) &&
//                                        (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
//                                    boolean result = isOutOrInLineConnectWinding(inline, transformerWindingRelateBus, true);
//                                    TransformerWinding transformerWinding = transformerWindingRelateBus.getTransformerWinding();
//                                    if (result){
//                                        transformerWindings.add(transformerWinding);
//                                    }
//                                }
//                            }
//                        }
                        
                        //一个主变和进现的连接
                        if (runningCount == 1){
                        	System.out.println("只有一个运行时主变，但是多条进线的情况stationName="+stationName);
                            List<TransformerWinding> onePower = transformerSubstation.getRunningTransformers().get(0).getWindings();
                            for(TransformerWinding transformerWinding: onePower){
                                String windType = transformerWinding.getWindType();
                                String yxClose = transformerWinding.getYx_close();
                                String bvId = transformerWinding.getBvId();

                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_HIGH.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvId)||Constants.BV_ID_35.equals(bvId))) {

                                        //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
//                                        if(!bvIdOutLine.equals(bvId)){
//                                            System.out.println("只有一个主变, 出线电压侧和主变电压侧不等！！！");
//                                            continue;
//                                        }
                                        transformerWindings.add(transformerWinding);
                                        System.out.println("只有一个主变，找到进线连接的主变侧");
                                    }
                                }
                            }
                        }else {
                            //多个主变和进线的连接
                            for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses){
                                //判断是否中低压输出侧
                                String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                                String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                                String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                                String powerName = transformerWindingRelateBus.getTransformerWinding().getName();

//                            //判断是否电压等级同侧，当然其实不同侧后面去找母线肯定也找不到，但是会出现同一条母线状态的不一致
//                            if(!bvIdOutLine.equals(bvIdWinding)){
//                                System.out.println("#####出现的电压等级和主变不一致！！！！powerName="+powerName);
//                                continue;
//                            }
                                //遥信闭合的
                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_HIGH.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                                        boolean result = isOutOrInLineConnectWinding(inline, transformerWindingRelateBus, true);
                                        TransformerWinding transformerWinding = transformerWindingRelateBus.getTransformerWinding();
                                        if (result){
                                            transformerWindings.add(transformerWinding);
                                        }
                                    }
                                }
                            }
                        }
                        //设置进线连接的主变记录
                        inline.setTransformerWindings(transformerWindings);
                        
                        // TODO:DEBUG 调试数据不全
                        List<TransformerWinding> windings = inline.getTransformerWindings();
                        int windingCount = (windings==null)?0:windings.size();
                        if (windingCount == 0) {
							System.out.println("inOutLinesToPower进线没有连接一个主变 stationName="+stationName);
							// TODO:DEBUG， 无奈加上一个主变吧
							List<TransformerWinding> onePower = transformerSubstation.getRunningTransformers().get(0).getWindings();
                            for(TransformerWinding transformerWinding: onePower){
                                String windType = transformerWinding.getWindType();
                                String yxClose = transformerWinding.getYx_close();
                                String bvId = transformerWinding.getBvId();
                                if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                                    //中低压侧且为110或者35KV的，因为还会出现6KV这种
                                    if ((Constants.WINDING_HIGH.equals(windType)) &&
                                            (Constants.BV_ID_110.equals(bvId)||Constants.BV_ID_35.equals(bvId))) {
//                                    	transformerWindings.add(transformerWinding);
//                                    	inline.setTransformerWindings(transformerWindings);
//                                    	System.out.println("没有连接一个主变, 手动添加主变成功 windingName="+transformerWinding.getName());
                                    }
                                }
                            }
						}
                    }
				}
            	
            }
        }
    }

    /**
     * 判断进出线是否和当前主变连接
     * @param inOutLines  进线或者出线
     * @param transformerWindingRelateBus 带有母线信息的主变某侧
     * @return
     */
    private boolean isOutOrInLineConnectWinding(InOutLines inOutLines, TransformerWindingRelateBus transformerWindingRelateBus, boolean isInLine){

        // TODO: 增加进线为线变组类型的处理逻辑
        //线变组的处理逻辑：不用找主变和母线的关系，在找进出线和主变关系时，可以根据主变的线变组标记来直接判断连接关系。
        // 通过线变组的ND找隔刀，然后找到间隔bayId,  进线线段找到End的间隔bayId，对比两者是否相等来确认 进线和主变的对应关系。
        if(isInLine){
            String isLineVariantGroup = transformerWindingRelateBus.getTransformerWinding().getIsLineVariantGroup();
            if ("1".equals(isLineVariantGroup)){
                //String windingNd = transformerWindingRelateBus.getTransformerWinding().getNd();
                String windingName = transformerWindingRelateBus.getTransformerWinding().getName();
                //Breaker breaker = findBreakerByNd(windingNd);
                // 直接用判断主变是否运行时找到的间隔ID来进行对比
                String bayIdForBus = transformerWindingRelateBus.getTransformerWinding().getBayIdForBus();
                if (null != bayIdForBus){
                    //String bayId = breaker.getBayId();
                    //找进线间隔
                    String segId = inOutLines.getAclnSegId();
                    String aclineId = inOutLines.getAclineId();
                    String sonStId = inOutLines.getSonStId();
                    String inLineBayId = null;
                    System.out.println("线变组windingName="+windingName+" segId="+segId+" aclineId="+aclineId+" bayIdForBus="+bayIdForBus);
                    if (null != segId){
                        inLineBayId = findBayIdBySegIdAndStId(segId, sonStId);
                    }else {
                        inLineBayId = findBayIdByAclineIdAndStId(aclineId, sonStId);
                    }

                    //如果通过segId或者aclineId找到的End的bayId为空，则需要再根据End的ND找隔刀的bayId
                    String disconnetorBayId = null;
                    if (null == inLineBayId) {
                    	String endNd = null;
                    	if (null != segId) {
                    		endNd = findNDBySegIdAndStId(segId, sonStId);
						}else {
							endNd = findNDByAclineIdAndStId(aclineId, sonStId);
						}
						
                    	Disconnector disconnector = findDisconnectorByNd(endNd);
                    	if (null != disconnector) {	
							disconnetorBayId = disconnector.getBayId();
						}
					}
                    
                    //对比两者是否相等,如果相等则代表连接，不相等则认为进线没带该主变。
                    if (null != bayIdForBus && bayIdForBus.equals(inLineBayId)){
                        System.out.println("线变组找到相连接的主变,通过End间隔对比");
                        return true;
                    }else if (null != bayIdForBus && bayIdForBus.equals(disconnetorBayId)) {		
                    	System.out.println("线变组找到相连接的主变,通过End中的ND找到的隔刀间隔对比");
                        return true;
                    }else{
                    	System.out.println("绕阻侧bayIdForBus="+bayIdForBus);
                        return false;
                    }
                }else {
                    System.out.println("#####isOutOrInLineConnectWinding 线变组之前在主变运行时没有找到bayIdForBus windingName="+windingName);
                    //return false;
                    // TODOD:DEBUG 调试数据不全，暂时返回true，以便找到主变
                    return true;
                }
            }
        }


        BusbarsectionStatus inOutLineBusStatus = inOutLines.getBusbarsectionStatus();
        //进出线母线
        List<Busbarsection> inOutLineBuses = null;
        //是否为一条母线状态
        boolean inOutLineOneBus = false;
        if (null != inOutLineBusStatus){
            inOutLineBuses = inOutLineBusStatus.getBusbarsections();
            inOutLineOneBus = inOutLineBusStatus.isEqualOneBus();
        }
        System.out.println("isOutOrInLineConnectWinding inOutLineBuses="+inOutLineBuses +" inOutLineOneBus="+inOutLineOneBus);


        BusbarsectionStatus busbarsectionStatus = transformerWindingRelateBus.getBusbarsectionStatus();
        boolean powerIsOneBus = false;
        List<Busbarsection> powerBuses = null;
        if (null != busbarsectionStatus){
            powerIsOneBus = busbarsectionStatus.isEqualOneBus();
            //主变连接的母线
            powerBuses = busbarsectionStatus.getBusbarsections();
        }
        System.out.println("isOutOrInLineConnectWinding powerIsOneBus="+powerIsOneBus+" powerBuses="+powerBuses);

        //如果是只有一条母线状态，主变供所有出线，所以要将改主变加入出线中
        if(powerIsOneBus){
            System.out.println("连上了transformerWindingRelateBus name="+transformerWindingRelateBus.getTransformerWinding().getName());
            return  true;
        }else {
            //查找各自相连的母线，看是否相连
            if((null != inOutLineBuses && inOutLineBuses.size()>0)&&
                    (null != powerBuses && powerBuses.size()>0)){
                for(Busbarsection busbarsection: inOutLineBuses){
                    for(Busbarsection bus: powerBuses){
                        if (busbarsection.equals(bus)){
                            //找到相同连接母线，则出线连接主变
                            System.out.println("出线和主变有母线相同");
                            System.out.println("连上了transformerWindingRelateBus name="+transformerWindingRelateBus.getTransformerWinding().getName());
                            return  true;
                        }
                    }
                }
            }
        }

        return false;
    }


    /**
     * 以父为供 key， 子为带value，构建供带关系
     * @param transformerSubstation  父站
     * @param children 子站集合
     * @return
     * @throws SQLException
     */

    public Map<List<PowerSupplyData>, List<PowerSupplyData>> ParentSupplyChild(TransformerSubstation transformerSubstation, 
    		List<TransformerSubstation> children) throws  SQLException{

        //一对一或者一对多
        Map<List<PowerSupplyData>, List<PowerSupplyData>> supplyBelt = new HashMap<List<PowerSupplyData>, List<PowerSupplyData>>();
        //父节点的出线集合
        List<InOutLines> outLines = transformerSubstation.getOutLines();
        //父站名字
        String parentStationName = transformerSubstation.getName();

        // TODO:DEBUG 调试数据不全
        int supplyBeltStationListCount = supplyBeltStationList.size();
        if (!supplyBeltStationList.contains(transformerSubstation)) {
        	supplyBeltStationList.add(transformerSubstation);
        	//System.out.println("ParentSupplyChild supplyBeltStationListCount="+supplyBeltStationListCount);
		}
        for(TransformerSubstation child : children){
            // TODO:DEBUG 调试数据不全
            if (!supplyBeltStationList.contains(child)) {
            	supplyBeltStationList.add(child);
            	//System.out.println("ParentSupplyChild supplyBeltStationListCount="+supplyBeltStationListCount);
    		}
            String childStationName = child.getName();
            //子节点的进线
            List<InOutLines> inLines = child.getInLines();
            //供
            InOutLines producer = null;
            //带
            InOutLines consumer = null;
            //记录看父子之间是否一条线都没有
            int outlineCount = (outLines==null)?0:outLines.size();
            System.out.println("parentStationName="+parentStationName+" childStationName="+childStationName
            		+" 父站出线条数："+outlineCount);
            int outlineIndex = 0;
            for(InOutLines outLine: outLines){
                //为每一条出现找供带
                //每一次将标志位复位
                boolean foundSegment = false;
                List<PowerSupplyData> producerData = new ArrayList<>();
                List<PowerSupplyData> consumerData = new ArrayList<>();
                //专门定义一对防重复的供带值
                List<PowerSupplyData> recordProducerData = new ArrayList<>();
                List<PowerSupplyData> recordConsumerData = new ArrayList<>();
                String segId = outLine.getAclnSegId();
                String aclineId = outLine.getAclineId();
                //判断是否是虚拟线，假设了同一对父子之间不会存在多条虚拟线
                boolean isVirtualLine = outLine.isVirtualLine();
                for(InOutLines inLine: inLines){
                	recordProducerData.clear();
                	recordConsumerData.clear();
                    String childSegId = inLine.getAclnSegId();
                    String childAclineId = inLine.getAclineId();
                    boolean childIsVirtualLine = inLine.isVirtualLine();
                    
                    if(null != segId && segId.equals(childSegId)){
                        System.out.println("找到线段 segId="+segId);
                        foundSegment = true;
                        producer = outLine;
                        consumer = inLine;
                        break;
                    }

                    if(null != aclineId && aclineId.equals(childAclineId)){
                        System.out.println("找到T接线 aclineId="+aclineId);
                        foundSegment = true;
                        producer = outLine;
                        consumer = inLine;
                        break;
                    }
                    
                    //同一个父子之间应该只有一条虚拟线
                    if (isVirtualLine && childIsVirtualLine) {
                        System.out.println("找到虚拟线");
                        foundSegment = true;
                        producer = outLine;
                        consumer = inLine;
                        break;
					}

                }

                //找到线段,记录供带关系数据
                if(foundSegment){
                    //出线侧为供
                    List<TransformerWinding> producerWindings = producer.getTransformerWindings();
                    //进线侧为带
                    List<TransformerWinding> consumerWindings = consumer.getTransformerWindings();
                    int producerWindingCount = (producerWindings==null) ?0: producerWindings.size();
                    int consumerWindingCount = (consumerWindings==null) ?0: consumerWindings.size();
                    System.out.println("----------------producerWindingCount="+producerWindingCount+" consumerWindingCount="+consumerWindingCount);

                    if((null != producerWindings && producerWindings.size()>0)&&
                            (null != consumerWindings && consumerWindings.size()>0)){

                        //供主变集合
                        for(TransformerWinding transformerWinding: producerWindings){
                            String windingName = transformerWinding.getName();
                            String number = Constants.getPowerNumber(windingName);
                            String stId = transformerWinding.getStId();
                            //注意主和备合并后，用此方法只会找到一个名字，所以前面一定要合并站，否则此处会造成key覆盖。
                            String stationName = findStationNameByStId(stId);
                            String bvId = findStationBvIdByStId(stId);
                            String voltage = Constants.convertBvIdToVoltage(bvId);
                            String mvanom = transformerWinding.getMvanom();
                            String trId = transformerWinding.getTrId();
                            
                            //增加通过绕阻ID查询遥测表字段
                            String windingId = transformerWinding.getId();
                            YCData ycData = YCQuery(windingId, Constants.COLUMN_ID_YC);
                            String historyTableName = ycData.getHistoryTableName();
                            String historyColumnName = ycData.getHistoryColumnName();

                            
                            PowerSupplyData produce = new PowerSupplyData();
                            produce.setStationName(stationName);
                            produce.setVoltage(voltage);
                            produce.setPowerNumber("#"+number);
                            produce.setCapacity(mvanom);
                            produce.setTrId(trId);
                            produce.setHistoryColumnName(historyColumnName);
                            produce.setHistoryTableName(historyTableName);
                            if (queryLoadForResult) {
                                String historyValue = null;
                                if (!queryedWindingYC.containsKey(windingId)) {
                                	historyValue = YCValueQuery(windingId, Constants.COLUMN_ID_YC, queryMoment);
                                	queryedWindingYC.put(windingId, historyValue);
								}else {
									historyValue = queryedWindingYC.get(windingId);
								}
                                produce.setHistoryValue(historyValue);
							}


                            producerData.add(produce);
                            recordProducerData.add(produce);
                        }

                        //带主变集合
                        for(TransformerWinding transformerWinding: consumerWindings){
                            String windingName = transformerWinding.getName();
                            String number = Constants.getPowerNumber(windingName);
                            String stId = transformerWinding.getStId();
                            String stationName = findStationNameByStId(stId);
                            String bvId = transformerWinding.getBvId();
                            String voltage = Constants.convertBvIdToVoltage(bvId);
                            String mvanom = transformerWinding.getMvanom();
                            String trId = transformerWinding.getTrId();
                            
                            //增加通过绕阻ID查询遥测表字段
                            String windingId = transformerWinding.getId();
                            YCData ycData = YCQuery(windingId, Constants.COLUMN_ID_YC);
                            String historyTableName = ycData.getHistoryTableName();
                            String historyColumnName = ycData.getHistoryColumnName();


                            PowerSupplyData consume = new PowerSupplyData();
                            consume.setStationName(stationName);
                            consume.setVoltage(voltage);
                            consume.setPowerNumber("#"+number);
                            consume.setCapacity(mvanom);
                            consume.setTrId(trId);
                            consume.setHistoryTableName(historyTableName);
                            consume.setHistoryColumnName(historyColumnName);
                            if (queryLoadForResult) {
                                String historyValue = null;
                                if (!queryedWindingYC.containsKey(windingId)) {
                                	historyValue = YCValueQuery(windingId, Constants.COLUMN_ID_YC, queryMoment);
                                	queryedWindingYC.put(windingId, historyValue);
								}else {
									historyValue = queryedWindingYC.get(windingId);
								}
                                consume.setHistoryValue(historyValue);
							}
                            

                            consumerData.add(consume);
                            recordConsumerData.add(consume);
                        }

                        //防止重复加入供带
                        RecordProducerAndConsumer recordProducerAndConsumer = new RecordProducerAndConsumer();
                        recordProducerAndConsumer.setProducerData(recordProducerData);
                        recordProducerAndConsumer.setConsumerData(recordConsumerData);
                        if (!hasFoundSupplyData.contains(recordProducerAndConsumer)) {
                        	hasFoundSupplyData.add(recordProducerAndConsumer);
                        	
                            //加入key value， 先判断是否已经有此key了
                            if(supplyBelt.containsKey(producerData)){
                                List<PowerSupplyData> existConsumerData = supplyBelt.get(producerData);
                                consumerData.addAll(existConsumerData);
                                supplyBelt.put(producerData, consumerData);
                            }else {
                                supplyBelt.put(producerData, consumerData);
                            }
						}

                    }
                    //一对父子之间可能存在多条线段，因此不能找到一条后就沾沾自喜地结束了
                    //break;
                }
     
            }
            
            //如果都没找到，可能需要分析
//            if(outlineCount == outlineIndex) {
//            	System.out.println("这对父子之间竟然没有找到线段！！！");
//            }

            //如果子节点还有子，则递归调用
            List<TransformerSubstation> grandsons = child.getChildren();
            if(null != grandsons && grandsons.size()>0 ){
                for(TransformerSubstation grandson: grandsons){
                    String grandsonName = grandson.getName();
                    System.out.println("grandsonName="+grandsonName);
                    Map<List<PowerSupplyData>, List<PowerSupplyData>> temp = ParentSupplyChild(child, grandsons);
                    if (null != temp){
                    	//System.out.println("grandson temp="+ temp.toString());
                        supplyBelt.putAll(temp);
                    }
                }
            }
        }
        
        //debug
//        if (parentStationName.equals("长沙.威灵变")) {
//        	System.out.println("#############################################");
//            Iterator<List<PowerSupplyData>> keyIterator = supplyBelt.keySet().iterator();
//            while (keyIterator.hasNext()) {
//            	List<PowerSupplyData> key = keyIterator.next();
//            	List<PowerSupplyData> value = supplyBelt.get(key);
//            	showSupplyBeltDataDebug(key);
//            	showSupplyBeltDataDebug(value);
//            }
//		}

        return supplyBelt;
    }

    

    /**
     *     显示供带，并返回生成文件地址
     * @param tomcatPath 部署的tomcat路径
     * @return
     */
    public String displayPowerSupplyRelation(String tomcatPath){

    	//FileWriter writer;
    	String filePathString = null;
    	String returnRelativePath = null;
    	try {
    		
        	String directory = supplybeltFilePath;
        	if(!tomcatPath.endsWith(File.separator)) {
        		tomcatPath = tomcatPath + File.separator;
        	}
        	File dir = new File(tomcatPath+directory);
        	if(!dir.exists()) {
        		dir.mkdirs();
        	}
        	
        	//替换时间里面的空格和冒号
        	String fileMoment = queryMoment;
        	fileMoment = fileMoment.replace(" ", "-");
        	fileMoment = fileMoment.replace(":", "-");
        	String fileName  = null;
        	//区分文件名称带有负载和不带负载的
        	if (queryLoadForResult) {
        		fileName  = "供带关系带有负载"+fileMoment+".txt";
			}else {
				fileName  = "供带关系不带负载"+fileMoment+".txt";
			}
        	
        	returnRelativePath = directory + File.separator + fileName;
        	filePathString = tomcatPath + returnRelativePath;
        	//设置写入字符编码, 覆盖方式写入文件
        	BufferedWriter writer = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (filePathString,false),"UTF-8"));
    		//writer = new FileWriter(filePathString);
    		//清空原先内容
    		writer.write("");
    		
    		   //一对一或者一对多
            Map<List<PowerSupplyData>, List<PowerSupplyData>> supplyBelt = new HashMap<List<PowerSupplyData>, List<PowerSupplyData>>();
            for (TransformerSubstation transformerSubstation : transformerSubstationList) {
                String bvIdSub = transformerSubstation.getBvId();
                Map<List<PowerSupplyData>, List<PowerSupplyData>> temp = null;
                //只要遍历查找220KV的站
                if(Constants.BV_ID_220.equals(bvIdSub)){
                    List<TransformerSubstation> children = transformerSubstation.getChildren();
                    temp = ParentSupplyChild(transformerSubstation, children);
                    System.out.println("");
                }
                if (null != temp){
                    supplyBelt.putAll(temp);
                }
            }
            
            //TODO:DEBUG 调试显示数据
            int count = 0;
            for(List<PowerSupplyData> key: supplyBelt.keySet()) {
            	count++;
            	for(PowerSupplyData keyData: key) {
            		String stationName = keyData.getStationName();
            		System.out.println("调试供数据展示 stationName="+stationName);
            	}
            	List<PowerSupplyData> value = supplyBelt.get(key);
            	for(PowerSupplyData valueData: value) {
            		String stationName = valueData.getStationName();
            		System.out.println("调试带数据展示 stationName="+stationName);
            	}        	
            }
            System.out.println("调试数据展示 count="+count);
            
            //展示Map的值
//            String titleString = "站名        主变编号         电压等级         容量           主变ID     分组编号        HISTORY_TABLE_NAME      HISTORY_COLUMN_NAME";
            String titleString = "站名 "+spaceString+"主变编号"+spaceString+"电压等级"+spaceString+
            		"容量"+spaceString+"主变ID"+spaceString+"分组编号"+spaceString+"HISTORY_TABLE_NAME"+
            		spaceString+"HISTORY_COLUMN_NAME"+spaceString+"遥测值";
            System.out.println(titleString);
        	writer.write(titleString);
        	writer.write("\n");
        	writer.flush();
            int groupNumber = 0;
            //第一个开始展示的需要为220KV的，否则会存在非220开始的供带，即使后面移除掉了子的
            //记录已经展示过的keys
            List<List<PowerSupplyData>> hasShowKeysList = new ArrayList<List<PowerSupplyData>>();
            //使用迭代器
            Iterator<List<PowerSupplyData>> keyIteratorOutside = supplyBelt.keySet().iterator();
            while (keyIteratorOutside.hasNext()) {
            	List<PowerSupplyData> keyOutside = keyIteratorOutside.next();
//            	if (hasShowKeysList.contains(keyOutside)) {
//            		//已经展示过了
//    				continue;
//    			}
            	//看看里面有没有220KV
            	int keyoutsideSize = (keyOutside==null) ?0: keyOutside.size();
            	int index = 0;
            	for(PowerSupplyData powerSupplyData: keyOutside) {
            		String bvId = powerSupplyData.getVoltage();
            		if ("220".equals(bvId)) {
    					break;
    				}
            		index++;
            	}
            	//如果key里面没有220KV， 则不显示，因为其为子，在后续的逻辑中会显示
//            	if (index == keyoutsideSize) {
//    				continue;
//    			}
//            	
            	
            	groupNumber++;
            	List<PowerSupplyData> value = supplyBelt.get(keyOutside);
            	String seperator  = "================================================================================================";
            	System.out.println(seperator);
            	writer.write(seperator);
            	writer.write("\n");
            	writer.flush();
            	//纯粹简单地显示供带
            	showSupplyBeltData(keyOutside, groupNumber, writer);
            	showSupplyBeltData(value, groupNumber, writer);
            	
            	// TODO:从220开始，各个key取到的value里面，找出各个站名，然后在key集合里面找是否有此站名
            	// 有的话直接跟在后面展示,一直递归找下去，直到所有取出来的value都在keys集合中找过了。
            	// 220开始的key也是如此，因为一个220站可能存在多个key value的情况。
//            	showSupplyBeltByKey(keyOutside, supplyBelt, groupNumber, writer);

            	//如果是只从220开始往下，一般220会带很多个站，需要对各个站集合进线遍历处理，即对value进线遍历
            	//showSupplyBeltByKey(value, supplyBelt, groupNumber, writer);
//            	for(PowerSupplyData eachStation: value) {
//            		List<PowerSupplyData> temp = new ArrayList<PowerSupplyData>();
//            		temp.add(eachStation);
//            		showSupplyBeltByKey(temp, supplyBelt, groupNumber, writer);
//            	}
            	
            	
                //使用迭代器,寻找可能存在的孙子节点
//                Iterator<List<PowerSupplyData>> keyIterator = supplyBelt.keySet().iterator();
//                while (keyIterator.hasNext()) {
//                	List<PowerSupplyData> valueIteratorAsKey = keyIterator.next();
//                	//如果值为key, 以value取其值
//                	if(value.equals(valueIteratorAsKey)){
//                        List<PowerSupplyData> valueChild = supplyBelt.get(valueIteratorAsKey);
//                        if(null != valueChild && valueChild.size()>0){
//                        	showSupplyBeltData(valueChild, groupNumber, writer);
//                            // 不移除，只是记录，然后在遍历的时候，对比，如果已经展示过了，就不再展示
//                            hasShowKeysList.add(valueIteratorAsKey);
//                        }
//                	}else {
//    					//也有可能是以该value集合中的某一个主变会供带孙节点
//                    	for(PowerSupplyData singleValueKey: value) {
//                    		List<PowerSupplyData> temp = new ArrayList<PowerSupplyData>();
//                    		temp.add(singleValueKey);
//                    		if (temp.equals(valueIteratorAsKey)) {
//                        		List<PowerSupplyData> valueChild = supplyBelt.get(valueIteratorAsKey);
//                                if(null != valueChild && valueChild.size()>0){
//                                	showSupplyBeltData(valueChild, groupNumber, writer);
//                                    // 验证是否会对前面的数据产生影响
//                                    hasShowKeysList.add(valueIteratorAsKey);
//                                }
//    						}
//                    	}
//    				}
//    			}
            }
            
            if (null != writer) {
            	writer.close();
			}
            
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("供带关系数据显示过程中出现异常");
			logger.error(e.getMessage());
		}
    	
        return returnRelativePath;
    }
    

    /**
      *  根据站名在所有供带数据里面找key，通过一个站名有的key都在每一次供带中找出来全部显示，其带的子value也是同样递归显示
     *  存在问题是子既作为带，又作为供，会重复显示
     * @param key
     * @param supplyBelt
     * @param groupNumber
     * @param writer
     */
    private void showSupplyBeltByKey(List<PowerSupplyData> key, 
    		Map<List<PowerSupplyData>, List<PowerSupplyData>> supplyBelt,
    		int groupNumber, BufferedWriter writer) {
    	if (null == key) {
			return;
		}
    	

    	//记录已经找过的站名，对于同一站多个主变，或者不同站的多个主变有防重复功能。
    	List<String> hasFoundStationName = new ArrayList<>();
    	//找站名
    	for(PowerSupplyData supplyData: key) {
    		//传入key的站名
    		String stationName = supplyData.getStationName();
    		System.out.println("传入需要查找的stationName="+stationName);
    		//防止同一个站多次显示
    		if (!hasFoundStationName.contains(stationName)) {
    			hasFoundStationName.add(stationName);
    	    	//记录同一个站拥有的key集合
    	    	List<List<PowerSupplyData>> sameStationKeyList = new ArrayList<>();
    	    	//所有key集合
    	    	Iterator<List<PowerSupplyData>> keyIterator = supplyBelt.keySet().iterator();
    			int recycleCount = 0;
        		while(keyIterator.hasNext()) {
        			List<PowerSupplyData> keyData = keyIterator.next();
        			//遍历所有key的站名，找出带有传入站名的
        			for(PowerSupplyData supplyData2: keyData) {
        				String stationName2 = supplyData2.getStationName();
        				if (stationName.equals(stationName2)) {
        					sameStationKeyList.add(keyData);
        					break;
    					}
        			}
        			recycleCount++;
        		}
        		System.out.println("遍历key循环次数recycleCount="+recycleCount);
        		
        		//将找的逐个key，取出value进行展示
        		for(List<PowerSupplyData> showKey: sameStationKeyList) {
        			List<PowerSupplyData> showValue = supplyBelt.get(showKey);
                	System.out.println("=====展示KEY  start=====");
                	showSupplyBeltData(showKey, groupNumber, writer);
                	System.out.println("=====展示KEY  end=====");
                	showSupplyBeltData(showValue, groupNumber, writer);

                	//TODO:如此展示能够合并220开始的站供带，但是对于子又有下一级供带会出现供重复，后续需要看是否可以
                	// 在递归调用时对写入文件行数进行查找处理，将子的递归供带中的带数据直接插入到原先的带后面。
                	//对于value进行递归调用
                	showSupplyBeltByKey(showValue, supplyBelt, groupNumber, writer);
        		}
			}
    		
    	}
    
    }
    
    /**
     * 分组展示供带数据
     * @param powerSupplyDatas
     * @param groupNumber
     */
    List<TransformerSubstation> showSupplyStationList = new ArrayList<TransformerSubstation>();
    private void showSupplyBeltData(List<PowerSupplyData> powerSupplyDatas, int groupNumber, BufferedWriter writer) {
    	if (null == powerSupplyDatas) {
			return ;
		}
    	
    	String dataString = null;
    	
        for(PowerSupplyData powerSupplyData: powerSupplyDatas){
            String stationName = powerSupplyData.getStationName();
            String powerNumber = powerSupplyData.getPowerNumber();
            String voltage = powerSupplyData.getVoltage();
            String capacity = powerSupplyData.getCapacity();
            String trId = powerSupplyData.getTrId();
            String historyTableName = powerSupplyData.getHistoryTableName();
            String historyColumnName = powerSupplyData.getHistoryColumnName();
            String historyValue = powerSupplyData.getHistoryValue();
            
            // TODO:DEBUG 调试数据不全
            TransformerSubstation transformerSubstation = findStationByName(stationName);
            if (null != transformerSubstation) {
                if (!showSupplyStationList.contains(transformerSubstation)) {
                	showSupplyStationList.add(transformerSubstation);
    			}
			}else {
				System.out.println("findStationByName is null stationName="+stationName);
			}


            dataString = stationName+spaceString +powerNumber+spaceString+voltage+spaceString+capacity+spaceString
                    +trId + spaceString+"G0"+groupNumber+spaceString+historyTableName
                    +spaceString+historyColumnName+spaceString+historyValue;
            System.out.println(dataString);
            try {
				writer.write(dataString);
				writer.write("\n");
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
    }
    
    
    
    private void showSupplyBeltDataDebug(List<PowerSupplyData> powerSupplyDatas) {
    	if (null == powerSupplyDatas) {
			return ;
		}
    	
    	String dataString = null;
    	
        for(PowerSupplyData powerSupplyData: powerSupplyDatas){
            String stationName = powerSupplyData.getStationName();
            String powerNumber = powerSupplyData.getPowerNumber();
            String voltage = powerSupplyData.getVoltage();
            String capacity = powerSupplyData.getCapacity();
            String trId = powerSupplyData.getTrId();
            String historyTableName = powerSupplyData.getHistoryTableName();
            String historyColumnName = powerSupplyData.getHistoryColumnName();

            dataString = stationName+spaceString +powerNumber+spaceString+voltage+spaceString+capacity+spaceString
                    +trId + spaceString+"G0"+spaceString+historyTableName
                    +spaceString+historyColumnName;
            System.out.println(dataString);
        }
        
    }



    // 在拓扑中，逐个遍历各个供主变输出侧的母线与出线的关系
    // 以及进线和该带主变输入侧的母线， 注意线变组的处理
    public void processInOutLinesWithBusRelation() throws  SQLException{
        for (TransformerSubstation transformerSubstation : transformerSubstationList) {
        	//站名
        	String stationName = transformerSubstation.getName();
            String bvIdSub = transformerSubstation.getBvId();
            //主变母线关系集合
            List<TransformerWindingRelateBus> transformerWindingRelateBusList = transformerSubstation.getTransformerWindingRelateBuses();
            //进出线集合,虚拟线已经单独处理了，不需要再此处找
            List<InOutLines> outLines = transformerSubstation.getOutLinesWitoutVirtual();
            List<InOutLines> inLines = transformerSubstation.getInLinesWitoutVirtual();
            int outlineCount = (null == outLines)?0:outLines.size();
            int inlineCount = (null == inLines)?0:inLines.size(); 
            System.out.println("处理进出线和母线关系 stationName="+stationName+" outlineCount="+outlineCount
            		+" inlineCount="+inlineCount);

            //输出侧的母线与出线关系, 220和110站输出，都还有要关注的35KV可能
            if(Constants.BV_ID_220.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
                //之前找的主变各侧和母线的关系集合数据，此处可以直接来用主变各侧找出线
                for(TransformerWindingRelateBus transformerWindingRelateBus:transformerWindingRelateBusList){
                    //判断是否中低压输出侧
                    String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                    String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                    String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //中低压侧且为110或者35KV的，因为还会出现6KV这种
                        if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                            //找出相同电压侧的出线,因为输出有中低压侧两种可能，对应的出现不同
                            List<InOutLines> sameBvIdOutLines = new ArrayList<>();
                            for(InOutLines outLine: outLines){
                                String bvId = outLine.getBvId();
                                if (bvIdWinding.equals(bvId)){
                                    sameBvIdOutLines.add(outLine);
                                }
                            }
                            System.out.println("processInOutLinesWithBusRelation sameBvIdOutLines size="+sameBvIdOutLines.size());
                            // 查找母线和出线之间的关系
                            BusbarsectionStatus busbarsectionStatus = transformerWindingRelateBus.getBusbarsectionStatus();
                            if (null!= busbarsectionStatus && busbarsectionStatus.isEqualOneBus()){
                                //如果是可以看做一条母线，此时找出线和母线的关系还是站内，即该主变连母线，母线连出线
                                //不需要找出线对应哪条母线， 各出线中记录母线只有一条
                                BusbarsectionStatus outLineBusStatus = new BusbarsectionStatus();
                                outLineBusStatus.setEqualOneBus(true);
                                //  要区分电压等级相同的侧
                                for(InOutLines outLine: sameBvIdOutLines){
                                    outLine.setBusbarsectionStatus(outLineBusStatus);
                                }
                            }else {
                                // 多条母线分开
                                //  此处需要的是主变某侧原始的母线集合
                                List<Busbarsection> busbarsections = busbarsectionStatus.getOrignalBuses();
                                setMultiBusWithInOutLines(sameBvIdOutLines, false, busbarsections);
                            }
                        }
                    }
                }
            }


            //输入侧的母线到进线的对应关系， 110 和 35 KV
            if(Constants.BV_ID_35.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub)){
                //之前找的主变各侧和母线的关系集合数据，此处可以直接来用主变各侧找出线
                for(TransformerWindingRelateBus transformerWindingRelateBus:transformerWindingRelateBusList){
                    //判断是否高压侧输入
                    String windType = transformerWindingRelateBus.getTransformerWinding().getWindType();
                    String bvIdWinding = transformerWindingRelateBus.getTransformerWinding().getBvId();
                    String yxClose = transformerWindingRelateBus.getTransformerWinding().getYx_close();
                    String windingName = transformerWindingRelateBus.getTransformerWinding().getName();
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //中低压侧且为110或者35KV的，因为还会出现6KV这种
                        if ((Constants.WINDING_HIGH.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                            // 查找母线和进线之间的关系
                            BusbarsectionStatus busbarsectionStatus = transformerWindingRelateBus.getBusbarsectionStatus();
                            if (null!= busbarsectionStatus && busbarsectionStatus.isEqualOneBus()){
                                //如果是可以看做一条母线，此时找进线和母线的关系还是站内，即该主变连母线，母线连进线
                                //不需要找进线对应哪条母线， 各进线中记录母线只有一条
                                BusbarsectionStatus inLineBusStatus = new BusbarsectionStatus();
                                inLineBusStatus.setEqualOneBus(true);
                                for(InOutLines inLine: inLines){
                                    inLine.setBusbarsectionStatus(inLineBusStatus);
                                }
                            }else {
                                // 多条母线分开
                                //  此处需要的是主变某侧原始的母线集合
                                List<Busbarsection> busbarsections = busbarsectionStatus.getOrignalBuses();
                                System.out.println("设置进线和母线多对多 windingName="+windingName);
                                setMultiBusWithInOutLines(inLines, true, busbarsections);
                            }
                        }
                    }
                }
            }
        }
    }

    
 
    /**
     * 通过隔刀找母线
     * @param disconnectors 隔刀集合
     * @param busbarsections 母线集合
     * @param busWithInOutLine 通过隔刀找到的母线集合
     * @throws SQLException
     */
    private void findBusByDisconnetors(List<Disconnector> disconnectors, List<Busbarsection>busbarsections,
            List<Busbarsection> busWithInOutLine) throws SQLException{
    	
    	if (null == busbarsections) {
			System.out.println("传入母线集合为空！！！");
			return;
		}
        
        if (null !=disconnectors && disconnectors.size() >0){
            //遍历母线里面的ND和查到的隔刀的ind或者jnd，如果有相等的，则查遥信
            //记录闭合的母线
            for(Busbarsection busbarsection: busbarsections){
                String nd = busbarsection.getNd();
                for(Disconnector disconnector: disconnectors){
                    String ind = disconnector.getInd();
                    String jnd = disconnector.getJnd();
                    if(nd.equals(ind) || nd.equals(jnd)){
                        String disconnetorId = disconnector.getId();
                        String disconnectorName = disconnector.getName();
                        boolean result = YXStatusEqualMoment(queryMoment,
                                disconnetorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
                        if (result){
                            //记录连接闭合的母线
                            busWithInOutLine.add(busbarsection);
                            System.out.println("findBusByDisconnetors 记录母线busbarsection name="+busbarsection.getName());
                        }else {
                        	System.out.println("findBusByDisconnetors 找到和母线连接的隔刀，但是遥信断开disconnectorName="+disconnectorName);
						}
                    }
                }
            }
        }else {
            System.out.println("findBusByDisconnetors 传入隔刀为空");
        }
    }

    
    private void findBusByNd(String nd, 
    		List<Busbarsection>busbarsections,
            List<Busbarsection> busWithInOutLine) throws SQLException{
    	
    	Disconnector disconnector = findDisconnectorByNd(nd);
    	if (null != disconnector) {
    		String bayIdInDis = disconnector.getBayId();
    		String disconnectorName = disconnector.getName();
    		System.out.println("setMultiBusWithInOutLines ND-隔刀 disconnectorName="+disconnectorName);
    		if (null != bayIdInDis) {
        		List<Disconnector> disconnectors2 = findDisconnectorByBayId(bayIdInDis);
        		if (null != disconnectors2 && disconnectors2.size()>0) {
        			findBusByDisconnetors(disconnectors2, busbarsections, busWithInOutLine);
				}else {
					System.out.println("setMultiBusWithInOutLines 继续通过ND找到隔刀, 但是bayIdInDis找隔刀集合为空 bayIdInDis="+bayIdInDis);
				}
			}else {
				//ND-隔刀-断路器-隔刀-母线
				//通过隔刀的ND找断路器，再由断路器的bayId找隔刀
				String ind = disconnector.getInd();
				String jnd = disconnector.getJnd();
				Breaker breaker = findBreakerByIndAndJnd(ind, jnd);
				if (null != breaker) {
					String breakerName = breaker.getName();
					String breakerBayId = breaker.getBayId();
					System.out.println("setMultiBusWithInOutLines ND-隔刀-断路器breakerName="+breakerName);
					if (null != breakerBayId) {
						List<Disconnector> disconnectors3 = findDisconnectorByBayId(breakerBayId);
						if (null != disconnectors3 && disconnectors3.size()>0) {
							findBusByDisconnetors(disconnectors3, busbarsections, busWithInOutLine);
						}else {
							System.out.println("setMultiBusWithInOutLines 通过断路器还是没有找到隔刀");
							// TODO: 用断路器的ind 或者jnd 找隔刀
							
						}
					}else {
						System.out.println("setMultiBusWithInOutLines 找断路器间隔ID为空");
					}
				}
			}

		}else {
			System.out.println("setMultiBusWithInOutLines 继续通过ND未找到隔刀");
		}
    }

    /**
     *多母线分开状态下，设置进出线与之关系
     * @param inOutLines 某个站的进线或者出线集合
     * @param isInline true为进线， false 为 出线
     * @param busbarsections 对应侧的母线原始数据集合
     * @throws SQLException
     */
    private void setMultiBusWithInOutLines(List<InOutLines> inOutLines,
                                           boolean isInline, List<Busbarsection> busbarsections)throws  SQLException{
    	System.out.println("setMultiBusWithInOutLines处理进出线和母线多对对 isInline="+isInline);
        for(InOutLines inoutLine: inOutLines){
            String aclnSegId = inoutLine.getAclnSegId();
            String aclineId = inoutLine.getAclineId();
            String parentStId = inoutLine.getParentStId();
            String sonStId = inoutLine.getSonStId();
            if (null == aclineId && null == aclnSegId){
                System.out.println("#####进出线中的aclnSegId和aclineId都为空！！！");
            }

            String stId = null;
            //进线，则用子节点的stId
            if (isInline){
                stId = sonStId;
            }else {
                //出现，则用父节点stId
                stId = parentStId;
            }
            
            //debug
            String parentName = findStationNameByStId(parentStId);
            String sonName = findStationNameByStId(sonStId);
            System.out.println("setMultiBusWithInOutLines parentName="+parentName+" sonName="+sonName);

            BusbarsectionStatus inoutLineBusStatus = new BusbarsectionStatus();
            inoutLineBusStatus.setEqualOneBus(false);
            //记录与进出现相连的母线记录
            List<Busbarsection> busWithInOutLine = new ArrayList<>();

            if (null != aclnSegId){
                //通过线段查线端，然后通过端的bayId查找间隔隔刀与母线连接情况
                String bayId = findBayIdBySegIdAndStId(aclnSegId, stId);
                if (null != bayId){
                    List<Disconnector> disconnectors = findDisconnectorByBayId(bayId);
                    if (null !=disconnectors && disconnectors.size() >0){
                    	System.out.println("setMultiBusWithInOutLines End--bayId--隔刀集合");
                    	findBusByDisconnetors(disconnectors, busbarsections, busWithInOutLine);
                    }else {
                    	// TODO: 通过End的 ND 继续找隔刀
                    	String nd = findNDBySegIdAndStId(aclnSegId, stId);
                    	System.out.println("setMultiBusWithInOutLines End--ND nd="+nd);
                    	findBusByNd(nd, busbarsections, busWithInOutLine);
//                    	Disconnector disconnector = findDisconnectorByNd(nd);
//                    	if (null != disconnector) {
//                    		String bayIdInDis = disconnector.getBayId();
//                    		String disconnectorName = disconnector.getName();
//                    		System.out.println("setMultiBusWithInOutLines ND-隔刀 disconnectorName="+disconnectorName);
//                    		if (null != bayIdInDis) {
//                        		List<Disconnector> disconnectors2 = findDisconnectorByBayId(bayIdInDis);
//                        		if (null != disconnectors2 && disconnectors2.size()>0) {
//                        			findBusByDisconnetors(disconnectors2, busbarsections, busWithInOutLine);
//    							}else {
//    								System.out.println("setMultiBusWithInOutLines 继续通过ND找到隔刀, 但是bayIdInDis找隔刀集合为空 bayIdInDis="+bayIdInDis);
//    							}
//							}else {
//								//ND-隔刀-断路器-隔刀-母线
//								//通过隔刀的ND找断路器，再由断路器的bayId找隔刀
//								String ind = disconnector.getInd();
//								String jnd = disconnector.getJnd();
//								Breaker breaker = findBreakerByIndAndJnd(ind, jnd);
//								if (null != breaker) {
//									String breakerName = breaker.getName();
//									String breakerBayId = breaker.getBayId();
//									System.out.println("setMultiBusWithInOutLines ND-隔刀-断路器breakerName="+breakerName);
//									if (null != breakerBayId) {
//										List<Disconnector> disconnectors3 = findDisconnectorByBayId(breakerBayId);
//										if (null != disconnectors3 && disconnectors3.size()>0) {
//											findBusByDisconnetors(disconnectors3, busbarsections, busWithInOutLine);
//										}else {
//											System.out.println("setMultiBusWithInOutLines 通过断路器还是没有找到隔刀");
//											// TODO: 用断路器的ind 或者jnd 找隔刀
//											
//										}
//									}else {
//										System.out.println("setMultiBusWithInOutLines 找断路器间隔ID为空");
//									}
//								}
//							}
//    
//						}else {
//							System.out.println("setMultiBusWithInOutLines 继续通过ND未找到隔刀");
//						}            	
					}
                
                }else{
                    System.out.println("#####通过segId和stId未找到间隔");
                }
            }else {
                //T接线
                String bayId = findBayIdByAclineIdAndStId(aclineId, stId);
                if (null != bayId){
                	System.out.println("setMultiBusWithInOutLines T接线 End--bayId--隔刀集合");
                    List<Disconnector> disconnectors = findDisconnectorByBayId(bayId);
                    if (null !=disconnectors && disconnectors.size() >0){
                    	findBusByDisconnetors(disconnectors, busbarsections, busWithInOutLine);
                    }else {
//                        System.out.println("T接线通过bayId未找到出线所在间隔的隔刀bayId="+bayId);
//                    	// TODO: 通过End的 ND 继续找隔刀
//                    	System.out.println("setMultiBusWithInOutLines T接线通过bayId未找到隔刀，继续通过ND查找");
                    	String nd = findNDByAclineIdAndStId(aclineId, stId);
                    	System.out.println("setMultiBusWithInOutLines T接线 End--ND nd="+nd);
                    	findBusByNd(nd, busbarsections, busWithInOutLine);
                    	
//                       	Disconnector disconnector = findDisconnectorByNd(nd);
//                    	if (null != disconnector) {
//                    		String bayIdInDis = disconnector.getBayId();
//                    		if (null != bayIdInDis) {
//                        		List<Disconnector> disconnectors2 = findDisconnectorByBayId(bayIdInDis);
//                        		if (null != disconnectors2 && disconnectors2.size()>0) {
//                        			findBusByDisconnetors(disconnectors2, busbarsections, busWithInOutLine);
//    							}else {
//    								System.out.println("setMultiBusWithInOutLines 继续通过ND找到隔刀, 但是bayIdInDis找隔刀集合为空 bayIdInDis="+bayIdInDis);
//    							}
//							}else {
//								//ND-隔刀-断路器-隔刀-母线
//								//通过隔刀的ND找断路器，再由断路器的bayId找隔刀
//								String ind = disconnector.getInd();
//								String jnd = disconnector.getJnd();
//								Breaker breaker = findBreakerByIndAndJnd(ind, jnd);
//								if (null != breaker) {
//									String breakerBayId = breaker.getBayId();
//									if (null != breakerBayId) {
//										List<Disconnector> disconnectors3 = findDisconnectorByBayId(breakerBayId);
//										if (null != disconnectors3 && disconnectors3.size()>0) {
//											findBusByDisconnetors(disconnectors3, busbarsections, busWithInOutLine);
//										}else {
//											System.out.println("setMultiBusWithInOutLines 通过断路器还是没有找到隔刀");
//											// TODO: 用断路器的ind 或者jnd 找隔刀
//											
//										}
//									}else {
//										System.out.println("setMultiBusWithInOutLines 找断路器间隔ID为空");
//									}
//								}
//							}
//    
//						}else {
//							System.out.println("setMultiBusWithInOutLines 继续通过ND未找到隔刀");
//						}
                    }
                }else{
                    System.out.println("#####通过aclineId和stId未找到间隔");
                }
            }

            //设置出线连接的母线集合
            inoutLineBusStatus.setBusbarsections(busWithInOutLine);
            inoutLine.setBusbarsectionStatus(inoutLineBusStatus);
            
            // TODO:DEBUG 调试数据不全
            boolean isOneBus = inoutLineBusStatus.isEqualOneBus();
            List<Busbarsection> foundBus = inoutLineBusStatus.getBusbarsections();
            int foudCount = (foundBus==null)?0:foundBus.size();
            if (!isOneBus && (foudCount==0)) {
				String stationName = findStationNameByStId(stId);
				if (isInline) {
					System.out.println("进线没有找到一条母线连接 stationName="+stationName+" segId="+aclnSegId
							+" aclineId="+aclineId);
				}else {
					System.out.println("出线没有找到一条母线连接 stationName="+stationName+" segId="+aclnSegId
							+" aclineId="+aclineId);
				}
				
			}
        }
    }

    //处理主变和母线的关系
    public void processPowerWindingWithBusRelation() throws  SQLException {
        for (TransformerSubstation transformerSubstation : transformerSubstationList) {
        	
        	//如果该站已经找过，就不要再重复找，添加进来
        	if (transformerSubstation.isHasFoundPowerToBus()) {
    			continue;
    		}
        	
            //各个站下面的运行时主变
            List<PowerTransformerWithWinding> runningTransformers = transformerSubstation.getRunningTransformers();
            String bvIdSub = transformerSubstation.getBvId();
            //主变母线关系集合
            List<TransformerWindingRelateBus> transformerWindingRelateBusList = transformerSubstation.getTransformerWindingRelateBuses();
            if (null == transformerWindingRelateBusList){
                transformerWindingRelateBusList = new ArrayList<>();
            }
            // 220kv 的站找中低压侧为110或者35的，其余不管
            // 110kv 的找高压侧输入的以及中低压输出为35的，其余不管
            // 35KV 的找高压侧输入的，其余不管

            // 找出进出线条数，如果出线只有一条，不需要查找主变输出侧到母线对应关系；如果进线只有一条，不需要找主变输入侧到母线关系
            List<InOutLines> outLines = transformerSubstation.getOutLines();
            List<InOutLines> inLines = transformerSubstation.getInLines();
            //进出线条数要算上虚拟线
            int outLineCount = (outLines==null) ? 0: outLines.size();
            int inLineCount = (inLines==null) ? 0: inLines.size();
            //出线集合
            List<InOutLines> outLinesWithoutVirtual = transformerSubstation.getOutLinesWitoutVirtual();
            List<InOutLines> inLinesWithoutVirtual = transformerSubstation.getInLinesWitoutVirtual();
            // 计算进出线条数的时候要算上虚拟线，否则一条情况为误判
            int outLineWithoutVirtualCount = (outLinesWithoutVirtual==null) ? 0: outLinesWithoutVirtual.size();
            int inLineWithoutVirtualCount = (inLinesWithoutVirtual==null) ? 0: inLinesWithoutVirtual.size();
            
            // TODO: 主变个数，如果只有一个主变，则有多条出线也无所谓，都由该主变供；有多条进线也无所谓，所有进线都带该主变
            String stationName = transformerSubstation.getName();
            System.out.println("processPowerWindingWithBusRelation stationName="+stationName+" outLineCount="+outLineCount
            		+" outLineWithoutVirtualCount="+outLineWithoutVirtualCount+" bvIdSub="+bvIdSub);
            System.out.println("processPowerWindingWithBusRelation stationName="+stationName+" inLineCount="+inLineCount
            		+" inLineWithoutVirtualCount="+inLineWithoutVirtualCount+" bvIdSub="+bvIdSub);

            // 输出侧到母线，包括220和110
            if((Constants.BV_ID_220.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub))){
                for (PowerTransformerWithWinding powerTransformerWithWinding : runningTransformers) {
                    //各个主变各侧的绕阻
                    List<TransformerWinding> windings = powerTransformerWithWinding.getWindings();
                    for (TransformerWinding transformerWinding : windings) {
                        String windType = transformerWinding.getWindType();
                        String bvIdWinding = transformerWinding.getBvId();
                        String yxClose = transformerWinding.getYx_close();
                        //遥信闭合的
                        if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                            //中低压侧且为110或者35KV的，因为还会出现6KV这种
                            if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                    (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                                // 查找主变和母线的关系
                                BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
                                // 虚拟线已经在前面找到跳过，不会再进来了，因此跑到这里的站都是无虚拟线
                                if (outLineCount == 1){
                                    busbarsectionStatus.setEqualOneBus(true);
                                    System.out.println("processPowerWindingWithBusRelation一条出线");
                                }else if(outLineCount>1){
                                    busbarsectionStatus = findPowerToBusRelation(transformerWinding);
                                }else {
                                    System.out.println("#######processPowerWindingWithBusRelation没有出线！！！！");
                                }
                                TransformerWindingRelateBus transformerWindingRelateBus = new TransformerWindingRelateBus();
                                transformerWindingRelateBus.setBusbarsectionStatus(busbarsectionStatus);
                                transformerWindingRelateBus.setTransformerWinding(transformerWinding);
                                transformerWindingRelateBusList.add(transformerWindingRelateBus);
                            }
                        }
                    }
                }
            }

            //输入侧到母线，包括110和35
            if((Constants.BV_ID_35.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub))){
                for (PowerTransformerWithWinding powerTransformerWithWinding : runningTransformers) {
                    //各个主变各侧的绕阻
                    List<TransformerWinding> windings = powerTransformerWithWinding.getWindings();
                    for (TransformerWinding transformerWinding : windings) {
                        String windType = transformerWinding.getWindType();
                        String bvIdWinding = transformerWinding.getBvId();
                        String yxClose = transformerWinding.getYx_close();
                        //遥信闭合的
                        if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                            //高压输入侧，电压等级类型和输出侧一直，因为出入都是相对的。
                            if ((Constants.WINDING_HIGH.equals(windType)) &&
                                    (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                                // 查找主变和母线的关系
                                BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
                                // TODO: 如果进出线只有一条，则将母线状态设置为一条，方面后面进出线找母线时使用主变找好的母线逻辑
                                if (inLineCount == 1){
                                    busbarsectionStatus.setEqualOneBus(true);
                                    System.out.println("processPowerWindingWithBusRelation一条进线");
                                }else if(inLineCount>1){
                                    busbarsectionStatus = findPowerToBusRelation(transformerWinding);
                                }else {
                                    System.out.println("#######processPowerWindingWithBusRelation没有进线！！！！");
                                }
                                TransformerWindingRelateBus transformerWindingRelateBus = new TransformerWindingRelateBus();
                                transformerWindingRelateBus.setBusbarsectionStatus(busbarsectionStatus);
                                transformerWindingRelateBus.setTransformerWinding(transformerWinding);
                                transformerWindingRelateBusList.add(transformerWindingRelateBus);
                            }
                        }
                    }
                }
            }
            // TODO: 一个站中是否要区分输出和输入侧？暂时在供带展示是根据wind_type判断高中低来区别
            //设置站主变和母线关系集合
            transformerSubstation.setTransformerWindingRelateBuses(transformerWindingRelateBusList);
            // TODO:DEBUG 调试数据不全
            for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBusList) {
            	BusbarsectionStatus busbarsectionStatus = transformerWindingRelateBus.getBusbarsectionStatus();
            	TransformerWinding winding = transformerWindingRelateBus.getTransformerWinding();
            	String windingName = null;
            	if (null != winding) {
            		windingName = winding.getName();
				}
            	//如果不是一条母线状态，而且没有母线，则输出
            	if (null != busbarsectionStatus){
            		//不为一条母线状态
            		if (!busbarsectionStatus.isEqualOneBus()) {
    					List<Busbarsection> busbarsections = busbarsectionStatus.getBusbarsections();
    					//母线集合为空
    					if (null == busbarsections || busbarsections.size() ==0) {
    						System.out.println("主变没有找到一条母线 windingName="+windingName);
    					}
					}
				}else {
					System.out.println("主变没有找到一条母线 windingName="+windingName);
				}
            }
            
        }
    }
    
   

    /**
     * 根据主变侧隔刀 和 母线，找到相连的母线
     * @param disconnectors
     * @param busbarsections
     * @return
     * @throws SQLException
     */
    private List<Busbarsection> findConnectBusByDisconnetors(List<Disconnector> disconnectors, List<Busbarsection> busbarsections) throws  SQLException{
    	//返回找到的和主变相连的母线集合
    	List<Busbarsection> needReturnBuses = new ArrayList<Busbarsection>();
        if (null != disconnectors && disconnectors.size()>0){
            //通过母线中的ND与主变查询到的隔刀jnd或者ind对比，确定连接情况
            for(Busbarsection busbarsection: busbarsections){
                String nd = busbarsection.getNd();
                //母线ID，用来记录主变与之连接关系
                String busName = busbarsection.getName();
                for (Disconnector disconnector: disconnectors){
                    String ind = disconnector.getInd();
                    String jnd = disconnector.getJnd();
                    if (nd.equals(ind)|| nd.equals(jnd)){
                        //找到与对应母线关联的隔刀，查遥信，确认是否连接
                        String disconnectorId = disconnector.getId();
                        String disconnectorName = disconnector.getName();
                        boolean disconnectorResult = YXStatusEqualMoment(queryMoment,
                                disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
                        if (disconnectorResult){
                            //记录母线
                            needReturnBuses.add(busbarsection);
                            System.out.println("记录通过隔刀找到的需要返回的母线busName="+busName);
                        }else {
                        	System.out.println("通过隔刀找到与母线相同的ND，但是隔刀断开disconnectorName="+disconnectorName);
						}
                    }
                }
            }
        }else {
        	System.out.println("隔刀传入为空");
		}
        
       return needReturnBuses;
    }

    /**
     * 针对2条母线只有一个断路器的情况
     * @param findBreaker  母联断路器
     * @param busbarsectionStatus 母线状态
     * @param busbarsections 原始母线集合
     * @param needReturnBuses 找到的母线集合
     * @param bayIdForBus  绕阻间隔ID
     * @throws SQLException
     */
    private void findBusStatusByBusBreaker(Breaker findBreaker, BusbarsectionStatus busbarsectionStatus,
    		List<Busbarsection> busbarsections,
    		List<Busbarsection> needReturnBuses, String bayIdForBus, String isVariantGroup) throws SQLException {
        //查遥信，确认两条母线是否相连
        String breakerId = findBreaker.getId();
        boolean result = YXStatusEqualMoment(queryMoment,
                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
        if (result){
            System.out.println("母联断路器闭合，不用记录主变和母线编号对应关系");
            busbarsectionStatus.setEqualOneBus(true);
        }else {
            System.out.println("母联断路器打开，查找主变对应哪条母线,找隔刀关联");
            busbarsectionStatus.setEqualOneBus(false);
            if(null != bayIdForBus){
                //根据主变的bay_id查找隔刀
                List<Disconnector> disconnectors = findDisconnectorByBayId(bayIdForBus);
                if (null != disconnectors && disconnectors.size()>0){
                	List<Busbarsection> temp  = findConnectBusByDisconnetors(disconnectors, busbarsections);
                	if (null != temp && temp.size()>0) {
						needReturnBuses.addAll(temp);
					}else {
						System.out.println("#####主变该侧找到的隔刀没有找到相连接的母线bayIdForBus="+bayIdForBus);
					}
                }else{
                    System.out.println("#####通过主变侧bayId未找到隔刀！！！！");
                    //增加通过bayId找断路器，然后通过断路器的ind或者jnd 找隔刀
                    Breaker breaker = findBreakerByBayId(bayIdForBus);
                    if (null != breaker) {
						String ind = breaker.getInd();
						String jnd = breaker.getJnd();
						List<Disconnector> disconnectors2 = findDisconnectorByIndAndJnd(ind, jnd);
						List<Busbarsection> temp  = findConnectBusByDisconnetors(disconnectors2, busbarsections);
                    	if (null != temp && temp.size()>0) {
							needReturnBuses.addAll(temp);
						}else {
//							System.out.println("bayId找到的断路器再找隔刀没有找到相连接的母线  transformerWinding name="+transformerWinding.getName());
							//直接与断路器nd比较
				            for(Busbarsection busbarsection: busbarsections){
				                String nd = busbarsection.getNd();
				                if (nd.equals(ind)|| nd.equals(jnd)) {
									//找到断路器，直接插遥信
				                	 String breakId = breaker.getId();
//				                	 System.out.println("bayId找到的断路器ND直接查找到了母线 breakId="+breakId
//				                	 		+ " transformerWinding name="+transformerWinding.getName());
				                	 boolean result2 = YXStatusEqualMoment(queryMoment,
				                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
				                	 if (result2) {
				                		 needReturnBuses.add(busbarsection);
									}else {
										System.out.println("母联断路器打开，没有与母线相连");
									}
								}
				            }
						}
					}
                }
            }else {
                System.out.println("主变侧bayId为空，是否需要继续查找 isVariantGroup="+isVariantGroup);
                // TODO: 这种是线变组，不用查母线关系了
            }
        }
    }
    
    

    /**
     * 查找主变某侧到母线的关系, 返回该主变相连的母线集合、母线原始条数集合、母线是否可以当成一条
     * @param transformerWinding
     * @return
     * @throws SQLException
     */
    public BusbarsectionStatus findPowerToBusRelation(TransformerWinding transformerWinding) throws  SQLException{
        //#找母线,根据站id和主变电压侧
        String stIdPower = transformerWinding.getStId();
        String powerName =transformerWinding.getName();
        String bvIdWinding = transformerWinding.getBvId();
        String bayIdForBus = transformerWinding.getBayIdForBus();
        String isVariantGroup = transformerWinding.getIsLineVariantGroup();

        BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
        //查找主变输出侧的母线
        List<Busbarsection> busbarsections = findBusbarsectionByBvIdAndstId(bvIdWinding, stIdPower);
        //只有为多条母线，并且断路器没有全部闭合时需要返回确定主变该侧连接的具体母线集合
        List<Busbarsection> needReturnBuses = new ArrayList<>();
        int busbarsectionCount = (null == busbarsections)?0:busbarsections.size();
        //查询母联断路器个数
        List<Breaker> breakers = findBreakerForBusbarsection(bvIdWinding, stIdPower);
        int breakerCount = (null == breakers)?0:breakers.size();
        System.out.println("powerName="+powerName+" 母线条数："+busbarsectionCount
        		+" 母联断路器个数："+breakerCount+" bayIdForBus="+bayIdForBus+" isVariantGroup="+isVariantGroup);
        
        if ((null != busbarsections)  && (busbarsections.size()>0)){
            if(busbarsections.size() == 1){
                //一条母线，不用找主变与母线的对应关系
                busbarsectionStatus.setEqualOneBus(true);
            }else if(busbarsections.size() == 2){
                //两条母线，找母联断路器
//                List<Breaker> breakers = findBreakerForBusbarsection(bvIdWinding, stIdPower);
                if (null!=breakers && breakers.size()>0){
                    if(breakers.size() == 1){
                        //查遥信，确认两条母线是否相连
                    	Breaker breaker = breakers.get(0);
                    	findBusStatusByBusBreaker(breaker, busbarsectionStatus, 
                    			busbarsections, needReturnBuses, bayIdForBus, isVariantGroup);
                    }else {
                        System.out.println("####活久见，两条母线查到多个母联断路器powerName="+powerName);
                    }
                }else {
                    //System.out.println("####2条母线未找到母联断路器#####powerName="+powerName);
                    // TODO: 一个断路器都没找到，直接根据编号找隔刀： 2条母线 110KV侧，隔刀关键字：5001 和 5002，  35kv侧 4001 和 4002；
                    //  3 条母线 110KV侧，隔刀关键字：5001 和 5002  35kv 4001 和 4002 I母和 II母； 隔刀关键字：5402 和 5404  35kv 4402 和 4404 II母和IV母
                    // 再根据找到的隔刀bayId ，再找断路器， 如果找不到断路器，直接使用隔刀的遥信。
                    String disconnectorNumber = null;
                    String disconnectorBayId;
                    if(Constants.BV_ID_110.equals(bvIdWinding)) {
                    	disconnectorNumber = "5001";
                    }else if (Constants.BV_ID_35.equals(bvIdWinding)) {
                    	disconnectorNumber = "4001";
					}
                    
                	Disconnector disconnector = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber);
                	if (null != disconnector) {
                		disconnectorBayId = disconnector.getBayId();
                        System.out.println("powerName="+powerName+" 2条母线找不到断路器用编号关键字找到隔刀disconnectorBayId="+disconnectorBayId);
                		Breaker breaker = findBreakerByBayId(disconnectorBayId);
                		if (null != breaker) {
							String breakerId = breaker.getId();
							System.out.println("通过隔刀编号关键字找到间隔，并找到断路器breakerId="+breakerId+" bvIdWinding="+bvIdWinding);
							findBusStatusByBusBreaker(breaker, busbarsectionStatus, busbarsections,
									needReturnBuses, null, isVariantGroup);
						}else {
							System.out.println("#####通过隔刀编号关键字找到间隔，并没有找到断路器，默认一条母线状态处理");
							// TODO: DEBUG 先暂时默认一条母线，补全数据
							busbarsectionStatus.setEqualOneBus(true);
						}
					}else {
						System.out.println("#####通过隔刀编号关键字找不到隔刀，默认一条母线状态处理");
						// TODO: DEBUG 先暂时默认一条母线，补全数据
						busbarsectionStatus.setEqualOneBus(true);
					}
                }
            }else if(busbarsections.size() == 3){
                //System.out.println("powerName="+powerName+" bvId="+bvIdWinding+" 主变侧3条母线");
                //三条母线其实都是单母，三分段或者环联，只要找到主变和那条母线相连即可，遥信都可不查，因为必连。
                // 然后根据断路器的个数以及遥信状态，来判断是否可以当成一条母线，以及其他情况。
                if(null != bayIdForBus){
                    //根据主变的bay_id查找隔刀，应该只会找到一把
                    List<Disconnector> disconnectors = findDisconnectorByBayId(bayIdForBus);
                    if (null != disconnectors && disconnectors.size()>0){
                        //通过母线中的ND与主变查询到的隔刀jnd或者ind对比，确定连接情况
                        for(Busbarsection busbarsection: busbarsections){
                            String nd = busbarsection.getNd();
                            //如果ND为空，不用看了
                            if (null == nd || "-1".equals(nd)) {
								continue;
							}
                            //母线ID，用来记录主变与之连接关系
                            String busId = busbarsection.getId();
                            for (Disconnector disconnector: disconnectors){
                                String ind = disconnector.getInd();
                                String jnd = disconnector.getJnd();
                                if (nd.equals(ind)|| nd.equals(jnd)){
                                    //找到与对应母线关联的隔刀，查遥信，确认是否连接
//                                    String disconnectorId = disconnector.getId();
//                                    boolean disconnectorResult = YXStatusEqualMoment(queryMoment,
//                                            disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
//                                    if (disconnectorResult){
//                                        //记录母线
//                                        needReturnBuses.add(busbarsection);
//                                        System.out.println("记录需要返回的母线busId="+busId);
//                                    }
                                    //应该是只有一条母线，因为这是单母
                                    needReturnBuses.add(busbarsection);
                                    System.out.println("3条母线找到主变侧连接的母线name="+busbarsection.getName());
                                    break;
                                }
                            }
                        }
                        
                        if (null == needReturnBuses || needReturnBuses.size()==0) {
                        	System.out.println("#####3条母线找到隔刀集合但是没有找到和主变相连的母线！！！");
						}
                    }else{
                        System.out.println("#####3条母线通过主变侧bayId未找到隔刀！！！！");
                        // TODO: 增加通过bayId找断路器，然后通过断路器的ind或者jnd 找隔刀
                    }
                }else {
                    System.out.println("主变侧bayId为空，是否需要继续查找 isVariantGroup="+isVariantGroup);
                    // TODO: 这种是线变组
                }

                //三条母线，找母联断路器，并且确定哪个断路器和哪两条母线相连
                //List<Breaker> breakers = findBreakerForBusbarsection(bvIdWinding, stIdPower);
                if (null!=breakers && breakers.size()>0) {
                    if (breakers.size() == 1){
                        String breakerId = breakers.get(0).getId();
                        boolean breakerResult = YXStatusEqualMoment(queryMoment,
                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                        if (breakerResult){
                            System.out.println("三母线只有一个断路器，闭合");
                        }else {
                            System.out.println("三母线只有一个断路器，断开");
                        }
                        //需要继续用关键字“母联”查找隔刀，判断遥信，然后与找到的断路器来确认三条母线连接状态
//                        Disconnector disconnector = findDisconnetorByNameAndBvIdStId(bvIdWinding, stIdPower);
//                        boolean disconnetorResult = false;
//                        if (null != disconnector){
//                            String disconnectorId = disconnector.getId();
//                            disconnetorResult = YXStatusEqualMoment(queryMoment,
//                                    disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
//                        }else {
//                            System.out.println("三母线只有一个断路器，隔刀未找到");
//                            // TODO: 没有找到隔刀，默认为闭合处理
//                            disconnetorResult = true;
//                        }
//                        
                        // 3 条母线 I母和 II母 隔刀关键字：110KV侧 5001 和 5002 , 35kv 4001 和 4002 ；II母和IV母 隔刀关键字：5402 和 5404  35kv 4402 和 4404 
                        // 再根据找到的隔刀bayId ，再找断路器， 如果找不到断路器，直接使用隔刀的遥信。
                        // 为了判断是否为一条母线状态，需要将四把隔刀全部查出来，查看非断路器连接的两条母线是否为闭合，并且查询到的断路器也是闭合
                        // 否则非一条母线状态，如果断路器为闭合，主变那侧连接的母线确认是否为断路器连接的两条中的一条，如果是，加入另外一条
                        // 如果为断路器为断开，则确认隔刀连接的另外两条是否闭合，如果是则确认是否包含主变侧连接的母线。
                        // I II 母
                        String disconnectorNumber1 = null;
                        String disconnectorNumber2 = null;
                        // II III 母
                        String disconnectorNumber3 = null;
                        String disconnectorNumber4 = null;
                        String disconnectorBayId;
                        if(Constants.BV_ID_110.equals(bvIdWinding)) {
                        	disconnectorNumber1 = "5001";
                        	disconnectorNumber2 = "5002";
                        	disconnectorNumber3 = "5402";
                        	disconnectorNumber4 = "5404";
                        }else if (Constants.BV_ID_35.equals(bvIdWinding)) {
                        	disconnectorNumber1 = "4001";
                        	disconnectorNumber2 = "4002";
                        	disconnectorNumber3 = "4402";
                        	disconnectorNumber4 = "4404";
    					}
                        
                        boolean disconnectorResult1 = false;
                        boolean disconnectorResult2 = false;
                        boolean disconnectorResult3 = false;
                        boolean disconnectorResult4 = false;
                        
                        Disconnector disconnector1 = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber1);
                        if (null != disconnector1) {
                        	System.out.println("三母线只有一个断路器找隔刀disconnector1="+disconnector1.getName());
							String disconnectorId = disconnector1.getId();
							disconnectorResult1 = YXStatusEqualMoment(queryMoment,
					        		   disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);			
						}
                        
                        Disconnector disconnector2 = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber2);
                        if (null != disconnector2) {
                        	System.out.println("三母线只有一个断路器找隔刀disconnector2="+disconnector2.getName());
							String disconnectorId = disconnector2.getId();
							disconnectorResult2 = YXStatusEqualMoment(queryMoment,
					        		   disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);			
						}
                        
                        Disconnector disconnector3 = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber3);
                        if (null != disconnector3) {
                        	System.out.println("三母线只有一个断路器找隔刀disconnector3="+disconnector3.getName());
							String disconnectorId = disconnector3.getId();
							disconnectorResult3 = YXStatusEqualMoment(queryMoment,
					        		   disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);			
						}
                        
                        Disconnector disconnector4 = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber4);
                        if (null != disconnector4) {
                        	System.out.println("三母线只有一个断路器找隔刀disconnector4="+disconnector4.getName());
							String disconnectorId = disconnector4.getId();
							disconnectorResult4 = YXStatusEqualMoment(queryMoment,
					        		   disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);			
						}
                        
                        //确认断路器连接的是哪两条母线
                        String breakerInd = breakers.get(0).getInd();
                        String breakerJnd = breakers.get(0).getJnd();
                        String breakerBayId = breakers.get(0).getBayId();
                        List<Busbarsection> breakerConnectBus = process3BusWith2Switch(breakerInd, breakerJnd, breakerBayId,needReturnBuses, busbarsections);
                        // II母在中间
                        boolean connectI = false;
                        boolean connectII = false;
                        boolean connectIII = false;
                        for(Busbarsection busbarsection: breakerConnectBus) {
                        	String busName = busbarsection.getName();
                        	if (busName.contains("Ⅰ")) {
                        		connectI = true;
							}
                        	if (busName.contains("Ⅱ")) {
                        		connectII = true;
							}
                        	if (busName.contains("Ⅳ") || busName.contains("Ⅲ")) {
                        		connectIII = true;
							}
                        }
                        
                        System.out.println("三母线只有一个断路器，其连接母线 connectI="+connectI
                        		+" connectII="+connectII+" connectIII="+connectIII);
                        
                        //判断断路器之外的另一对母线连接情况
                        boolean disconnetorResult = false;
                        //断路器连接了I II 母
                        if (connectI) {
                        	//取隔刀 II III 母情况
                        	disconnetorResult = disconnectorResult3 && disconnectorResult4;
						}
                        
                        if (connectIII) {
                        	//取隔刀 I II 母情况
                        	disconnetorResult = disconnectorResult1 && disconnectorResult2;
						}

                        System.out.println("三母线只有一个断路器breakerResult="+breakerResult+" disconnetorResult="+disconnetorResult);
                        if(breakerResult && disconnetorResult){
                            //两者都闭合，则为一条母线状态
                            busbarsectionStatus.setEqualOneBus(true);
                            System.out.println("三母一断一隔刀都闭合，三连状态");
                        }else {
                            //   找到主变连接的母线
                            // 如果两个都开，则只要找到主变连接的那条，对于单母，可以不用查遥信，记录即可
                            // 如果是一个开，一个闭，则找到和主变相连的那条后，
                            // 还要通过断路器或者隔刀的bayId查找出ND数据，判断闭合那个开关是否和主变连接的那条母线相连，是，则加入记录
                            busbarsectionStatus.setEqualOneBus(false);
                            //两个都开，不用管了，前面已经记录了主变连接的母线
                            if (!breakerResult && !disconnetorResult){
                                System.out.println("三母一断一隔刀都断开，三开状态");
                            }else{
                                if( null != needReturnBuses &&  needReturnBuses.size()>0){
                                    if (breakerResult){
                                        String ind = breakers.get(0).getInd();
                                        String jnd = breakers.get(0).getJnd();
                                        String bayId = breakers.get(0).getBayId();
                                        process3BusWith2Switch(ind, jnd, bayId,needReturnBuses, busbarsections);
                                    }else {
                                        //隔刀闭合，断路器断开
//                                        if(null != disconnector){
//                                            String ind = disconnector.getInd();
//                                            String jnd = disconnector.getJnd();
//                                            String bayId = disconnector.getBayId();
//                                            process3BusWith2Switch(ind, jnd, bayId,needReturnBuses, busbarsections);
//                                        }
                                    	//如果断路器连接的是I II母，断开，则看II III母由隔刀闭合的是否其中有和主变连接的，然后加上另外一条
                                    	Busbarsection powerConnectOne = needReturnBuses.get(0);
                                    	String powerConnectBusName = powerConnectOne.getName();
                                    	if (connectI) {
											if (powerConnectBusName.contains("Ⅱ")) {
												//添加III母
												for(Busbarsection busbarsection: busbarsections) {
													String busName = busbarsection.getName();
													if (busName.contains("Ⅳ")||busName.contains("Ⅲ")) {
														needReturnBuses.add(busbarsection);
													}
												}
											}
											
											
											if (powerConnectBusName.contains("Ⅳ") || powerConnectBusName.contains("Ⅲ")) {
												//添加III母
												for(Busbarsection busbarsection: busbarsections) {
													String busName = busbarsection.getName();
													if (busName.contains("Ⅱ")) {
														needReturnBuses.add(busbarsection);
													}
												}
											}

										}

                                    	if (connectIII) {
											if (powerConnectBusName.contains("Ⅰ")) {
												//添加III母
												for(Busbarsection busbarsection: busbarsections) {
													String busName = busbarsection.getName();
													if (busName.contains("Ⅱ")) {
														needReturnBuses.add(busbarsection);
													}
												}
											}
											
											
											if (powerConnectBusName.contains("Ⅱ")) {
												//添加III母
												for(Busbarsection busbarsection: busbarsections) {
													String busName = busbarsection.getName();
													if (busName.contains("Ⅰ")) {
														needReturnBuses.add(busbarsection);
													}
												}
											}

										}
                                    }
                                }else{
                                    System.out.println("####单母三分段没有找到和主变相连的母线！！！！");
                                }
                            }
                        }
                    }else if (breakers.size() == 2) {
                        //查遥信，确认三条条母线是否相连
                        String breaker1Id = breakers.get(0).getId();
                        String breaker2Id = breakers.get(1).getId();
                        boolean result1 = YXStatusEqualMoment(queryMoment,
                                breaker1Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result2 = YXStatusEqualMoment(queryMoment,
                                breaker2Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        if (result1 && result2) {
                            System.out.println("母联断路器都闭合，不用记录主变和母线编号对应关系");
                            busbarsectionStatus.setEqualOneBus(true);
                        }else {
                            //首先找到与主变相连的那条母线，那条母线ND包含在主变同一间隔的隔刀中的ind或者jnd中
                            busbarsectionStatus.setEqualOneBus(false);
                            if(!result1 && !result2){
                                //如果都断开，记录此母线即可
                                System.out.println("三母二断路器都断开，三开状态");
                            }else {
                                //如果是一个断开，一个闭合，则需要确认闭合的那个断路器是否连接了主变那侧出来相连的母线
                                if( null != needReturnBuses &&  needReturnBuses.size()>0){
                                    if (result1){
                                        //breaker1 闭合
                                        String ind = breakers.get(0).getInd();
                                        String jnd = breakers.get(0).getJnd();
                                        String bayId = breakers.get(0).getBayId();
                                        process3BusWith2Switch(ind, jnd, bayId,needReturnBuses, busbarsections);
                                    }else {
                                        //breaker2 闭合
                                        String ind = breakers.get(1).getInd();
                                        String jnd = breakers.get(1).getJnd();
                                        String bayId = breakers.get(1).getBayId();
                                        process3BusWith2Switch(ind, jnd, bayId, needReturnBuses, busbarsections);
                                    }
                                }
                            }
                        }
                    } else if(breakers.size() == 3){
                        //查遥信，确认三条条母线是否相连
                        String breaker1Id = breakers.get(0).getId();
                        String breaker2Id = breakers.get(1).getId();
                        String breaker3Id = breakers.get(2).getId();
                        boolean result1 = YXStatusEqualMoment(queryMoment,
                                breaker1Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result2 = YXStatusEqualMoment(queryMoment,
                                breaker2Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result3 = YXStatusEqualMoment(queryMoment,
                                breaker3Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        //其中任意两个闭合
                        if(result1&&result2 || result2&&result3 || result3&&result1){
                            System.out.println("母联断路器都闭合，不用记录主变和母线编号对应关系");
                            busbarsectionStatus.setEqualOneBus(true);
                        }else {
                            //找出主变侧连接的那条母线，记录
                            busbarsectionStatus.setEqualOneBus(false);
                            System.out.println("三母线三断路器，两个以上断开，则不用处理，前面已经找到和主变相连的母线");
                        }
                    }else {
                        System.out.println("三条母线，断路器个数=" + breakers.size());
                    }
                }else{
                    System.out.println("#####三条母线，没有找到母联断路器");
                }
            }else if(busbarsections.size() == 4){
                System.out.println("powerName="+powerName+" bvId="+bvIdWinding+" 主变侧4条母线");
                //同三条
                // TODO:DEBUG 调试数据不全
                busbarsectionStatus.setEqualOneBus(true);
            }else {
                System.out.println("####活久见找到超过4条母线#####powerName="+powerName);
                // TODO:DEBUG 调试数据不全
                busbarsectionStatus.setEqualOneBus(true);
            }
            for(Busbarsection busbarsection: busbarsections){
                System.out.println("母线名称="+busbarsection.getName());
            }
        }else {
            System.out.println("####未找到主变侧母线#####powerName="+powerName+" isVariantGroup="+isVariantGroup);
            //线变组逻辑在进线和主变的对应关系中已经处理
            // TODO: DEBUG 暂时默认为只有一条母线处理
            busbarsectionStatus.setEqualOneBus(true);
        }
        busbarsectionStatus.setBusbarsections(needReturnBuses);
        busbarsectionStatus.setOrignalBuses(busbarsections);
        
        //TODO:DEBUG 增加调试数据，显示最终找到的母线状态
        boolean isOneBus = busbarsectionStatus.isEqualOneBus();
        if (isOneBus) {
        	System.out.println("一条母线状态 powerName="+powerName);
		}
        List<Busbarsection> foundBuses = busbarsectionStatus.getBusbarsections();
        //显示既不是一条母线状态，也没有找到关联母线的主变
        if (!isOneBus) {
			if (foundBuses == null || foundBuses.size()==0) {
				System.out.println("既不是一条母线状态，也没有找到关联母线的主变 powerName="+powerName);
			}
		}
        return busbarsectionStatus;
    }


    /**
     *处理单母三分段中有两个开关，一个闭一个合状态， 返回闭合的这个连接的两条母线
     * @param ind, jnd, 隔刀或者断路器数据
     * @param needReturnBuses  查询到的主变和母线相连的母线
     * @param busbarsections  总母线集合
     */
    private List<Busbarsection> process3BusWith2Switch(String ind, String jnd, String bayId,
                                        List<Busbarsection> needReturnBuses, List<Busbarsection> busbarsections){
        //查找 相连的两条母线
        List<Busbarsection> ConnectBuses = new ArrayList<>();
        //找到断路器或者隔刀同间隔的隔刀
        List<Disconnector> disconnectorsWithSwith = new ArrayList<>();

        //通过间隔ID找到隔刀集合
        List<Disconnector> disconnectors = findDisconnectorByBayId(bayId);
        if (null != disconnectors && disconnectors.size()>0) {
            for(Disconnector disconnector: disconnectors){
                String disInd = disconnector.getInd();
                String disJnd = disconnector.getJnd();
                //找到断路器或者隔刀同间隔的隔刀
                if (disInd.equals(ind) || disInd.equals(jnd)){
                    disconnectorsWithSwith.add(disconnector);
                }

                if (disJnd.equals(ind)|| disJnd.equals(jnd)){
                    disconnectorsWithSwith.add(disconnector);
                }
            }
		}

        //再用找到的隔刀集合来找相连的母线
        for(Disconnector disconnector: disconnectorsWithSwith){
            String switchInd = disconnector.getInd();
            String switchJnd = disconnector.getJnd();
            for(Busbarsection busbarsection: busbarsections){
                String nd = busbarsection.getNd();
                if (nd.equals(switchInd) || nd.equals(switchJnd)){
                    ConnectBuses.add(busbarsection);
                }
            }
        }

        //遍历完之后，如果 连接的两条母线中包含主变连接的那条，则这两条都要加入
        //并且去重复
        if (null != needReturnBuses && needReturnBuses.size()>0) {
            Busbarsection connectPowerBus = needReturnBuses.get(0);
            if (ConnectBuses.contains(connectPowerBus)){
                for(Busbarsection busbarsection: ConnectBuses){
                    if (!needReturnBuses.contains(busbarsection)){
                        needReturnBuses.add(busbarsection);
                    }
                }
            }else {
                System.out.println("该开关没有连接和主变相连的母线");
            }
		}

        return ConnectBuses;

    }

//--------------------------------------------------------------------------------------------------//

//--------------------------------------------------------------------------------------------------//
    //所有在构建拓扑中查找过的站，包括常规构建，T， 电压等级相等的
    List<TransformerSubstation> NodeInTree = new ArrayList<TransformerSubstation>();
    //从上往下构建拓扑
    public void genTree(List<TransformerSubstation> parent, List<TransformerSubstation> son) {
        for(TransformerSubstation parentNode: parent){
            String stIdParent = parentNode.getStId();
            List<TransformerSubstation> children = new ArrayList<>();
            String parentName = parentNode.getName();
            //debug
            if (DMQuery.ST_ID_JINWANZI_COMBINE.equals(stIdParent)) {
				System.out.println("井湾子合并站作为父站来过....");
			}
            //记录父节点的出线集合
            List<InOutLines> outLines = parentNode.getOutLinesWitoutVirtual();
            if (null == outLines){
                outLines = new ArrayList<>();
            }
            //父节点的线段集合
            List<ACLineEndWithSegment> parentEndWithSegments = parentNode.getEndWithSegments();
            for (TransformerSubstation sonNode: son){
                List<ACLineEndWithSegment> sonEndWithSegments = sonNode.getEndWithSegments();
                String stIdSon = sonNode.getStId();
                String sonName = sonNode.getName();

                //记录子节点的进线集合
                List<InOutLines> inLines = sonNode.getInLinesWitoutVirtual();
                if (null == inLines){
                    inLines = new ArrayList<>();
                }

                for(ACLineEndWithSegment acLineEndWithSegment: sonEndWithSegments){
                    //线段里面的首站为父节点stId的则找到供电爸爸站了
                    String istId = acLineEndWithSegment.getIstId();
                    // TODO: 也许可以将逻辑修改为istId 或者 jstId 等于stIdParent
                    String jstId = acLineEndWithSegment.getJstId();

                    if (stIdParent.equals(istId) || 
                    		stIdParent.equals(jstId)){
                        // TODO: 将此线段作为出现放入父节点，作为进线放入子节点
                        String aclnSegId = acLineEndWithSegment.getAclnsegId();
                        String aclineId = acLineEndWithSegment.getAclineId();
                        String sonBvId = acLineEndWithSegment.getBvId();

                        // TODO: 别偷懒，进线和出现使用两个对象
                        InOutLines outline = new InOutLines();
                        outline.setParentStId(stIdParent);
                        outline.setSonStId(stIdSon);
                        outline.setAclnSegId(aclnSegId);
                        outline.setAclineId(aclineId);
                        //为了区分中低压侧输出，是110还是35KV侧，设置电压等级
                        outline.setBvId(sonBvId);

                        InOutLines inline = new InOutLines();
                        inline.setParentStId(stIdParent);
                        inline.setSonStId(stIdSon);
                        inline.setAclnSegId(aclnSegId);
                        inline.setAclineId(aclineId);
                        //为了区分中低压侧输出，是110还是35KV侧，设置电压等级
                        inline.setBvId(sonBvId);

                        inLines.add(inline);
                        outLines.add(outline);

                        children.add(sonNode);
                        // TODO: DEBUG
                        if (!NodeInTree.contains(parentNode)) {
                        	NodeInTree.add(parentNode);
						}
                        if (!NodeInTree.contains(sonNode)) {
                        	NodeInTree.add(sonNode);
						}
                        System.out.println("父站添加出线 parentName="+parentName+
                        		" 子站添加进线 sonName= "+sonName+" aclnSegId="+aclnSegId
                        		+" aclineId="+aclineId);
                        //找到之后，剩余线段也不用看了
                        break;
                    }
                }
                //设置子节点进线集合
                sonNode.setInLines(inLines);
            }
            //将儿子都归队
            parentNode.setChildren(children);
            //设置父节点出现集合
            parentNode.setOutLines(outLines);
        }
    }



    /**
     *
     * @param bvIdKindList  当前T包含的电压种类集合
     * @param substationWithSegments  当前T包含的站集合，带有线端信息
     */
    public void processTNodeWith2KindBvId(List<String> bvIdKindList, List<SubstationWithSegment> substationWithSegments){

        //找出小于220KV的结点作为子节点
        List<TransformerSubstation> parent = new ArrayList<>();
        List<TransformerSubstation> son = new ArrayList<>();

        if (bvIdKindList.contains(Constants.BV_ID_220)){
            System.out.println("processTNodeWith2KindBvId 包含220");
            //debug 调试查看有多少个T里面高电压有两个及以上同时闭合的情况
            int index=0;
            for(SubstationWithSegment substationWithSegment: substationWithSegments){
                String bvId = substationWithSegment.getBvId();
                String stId = substationWithSegment.getId();
                for(TransformerSubstation transformerSubstation: transformerSubstationList){
                    String stIdInSub = transformerSubstation.getStId();
                    if (stId.equals(stIdInSub)){
                        if (Constants.BV_ID_220.equals(bvId)){
                            parent.add(transformerSubstation);
                            index++;
                        }else{
                            son.add(transformerSubstation);
                        }
                        break;
                    }
                }
            }
            
            if (index>=2) {
            	System.out.println("processTNodeWith2KindBvId 包含2个及以上的220站");
			}
                       
        }else if(bvIdKindList.contains(Constants.BV_ID_110)){
            System.out.println("processTNodeWith2KindBvId 包含110");
            //debug 调试查看有多少个T里面高电压有两个及以上同时闭合的情况
            int index=0;
            for(SubstationWithSegment substationWithSegment: substationWithSegments){
                String bvId = substationWithSegment.getBvId();
                String stId = substationWithSegment.getId();
                for(TransformerSubstation transformerSubstation: transformerSubstationList){
                    String stIdInSub = transformerSubstation.getStId();
                    if (stId.equals(stIdInSub)){
                        if (Constants.BV_ID_110.equals(bvId)){
                            parent.add(transformerSubstation);
                            index++;
                        }else{
                            son.add(transformerSubstation);
                        }
                        break;
                    }
                }
            }
            
            if (index>=2) {
            	System.out.println("processTNodeWith2KindBvId 包含2个及以上的110站");
			}
        }else if(bvIdKindList.contains(Constants.BV_ID_35)){
            System.out.println("processTNodeWith2KindBvId 包含35");
            for(SubstationWithSegment substationWithSegment: substationWithSegments){
                String bvId = substationWithSegment.getBvId();
                String stId = substationWithSegment.getId();
                for(TransformerSubstation transformerSubstation: transformerSubstationList){
                    String stIdInSub = transformerSubstation.getStId();
                    if (stId.equals(stIdInSub)){
                        if (Constants.BV_ID_35.equals(bvId)){
                            parent.add(transformerSubstation);
                        }else{
                            son.add(transformerSubstation);
                        }
                        break;
                    }
                }
            }
        }else{
            System.out.println("10KV的已经被当做儿子处理了");
        }

        //遍历完之后，构建拓扑
        System.out.println("parent size="+parent.size());
        //将aclineId记录到父子出进线数据结构中
        String aclineId = null;
        for(SubstationWithSegment substationWithSegment: substationWithSegments){
            aclineId = substationWithSegment.getAcLineEndWithSegment().getAclineId();
            break;
        }
        for(TransformerSubstation father: parent){
            //找出原先构建好的父子拓扑，合并后再设置
            List<TransformerSubstation> children = father.getChildren();
            if(null == children){
                children = new ArrayList<>();
            }
            //查询出父节点出线信息
            String parentStId = father.getStId();
            String parentName = father.getName();
            List<InOutLines> outLines = father.getOutLinesWitoutVirtual();
            if (null == outLines){
                outLines = new ArrayList<>();
            }
            //遍历子
            for(TransformerSubstation child: son){
                // TODO: 查询父子节点，线段id, aclineId, 将进线加入child
                //查询出子节点进线信息
                String sonStId = child.getStId();
                String sonName = child.getName();
                List<InOutLines> inLines = child.getInLinesWitoutVirtual();
                if (null == inLines){
                    inLines = new ArrayList<>();
                }
                // TODO: 进出线变量分开
                //将子节点的电压等级设置到进出线中，因为出现有中低压侧区分
                String sonBvId = child.getBvId();
                InOutLines outline = new InOutLines();
                outline.setParentStId(parentStId);
                outline.setSonStId(sonStId);
                outline.setAclineId(aclineId);
                outline.setBvId(sonBvId);

                InOutLines inline = new InOutLines();
                inline.setParentStId(parentStId);
                inline.setSonStId(sonStId);
                inline.setAclineId(aclineId);
                inline.setBvId(sonBvId);

                inLines.add(inline);
                outLines.add(outline);
                //设置子节点进线，不管这个子是否已经添加到父了，进线有多少就加多少
                child.setInLines(inLines);
                
                // TODO: DEBUG 调试数据缺失
                if (!NodeInTree.contains(father)) {
                	NodeInTree.add(father);
				}
                if (!NodeInTree.contains(child)) {
                	NodeInTree.add(child);
				}
                System.out.println("T接线父站添加出线 parentName="+parentName+
                		" 子站添加进线 sonName= "+sonName +" aclineId="+aclineId);
                
                if(!children.contains(child)){
                    System.out.println("children add son ");
                    children.add(child);                  
                }else {
                    System.out.println("children contains son already ");
                    //  TODO: 已经包含了也要处理，一个T就代表这一条实际的线，会出现一对父子有多条线段的情况
                    // 例如寺冲 和 环保，常规线带#2主变 T带#1主变
                }
            }

            father.setChildren(children);
            //设置父节点出线集合
            father.setOutLines(outLines);
        }
    }

    //构建T接线拓扑
    public void genTreeForTNode(){
        for (ACLineWithSubstation acLineWithSubstation: acLineWithSubstationList){
            String aclineId = acLineWithSubstation.getAclineId();

            List<SubstationWithSegment> substationWithSegments = acLineWithSubstation.getSubstationWithSegments();
            if (null != substationWithSegments && substationWithSegments.size() > 1){
                System.out.println("T接线 acline_id ："+aclineId);
                //记录此T接线的电压等级种类，如果有不同类型的，则可以直接确定父子关系
                List<String> bvIdKindList = new ArrayList<>();
                for(SubstationWithSegment substationWithSegment: substationWithSegments){
                    String stIdInT = substationWithSegment.getId();
                    //因为之前线段表的集合中是从线端过来，已经去除了虚拟站，所以加入到此集合中的stId应该都是常规站
                    //根据stId 遍历站，获取bvId， 如果其中有某一个比其他电压高，则其为父，其余为子
                    for(Substation sub: substationList){
                        String stIdInSub = sub.getId();
                        if (stIdInT.equals(stIdInSub)){
                            String bvId = sub.getBvId();
                            String name = sub.getName();
                            substationWithSegment.setBvId(bvId);
                            substationWithSegment.setName(name);
                            System.out.println("T接线 name="+ name +" bvId ："+ bvId);
                            if (!bvIdKindList.contains(bvId)){
                                bvIdKindList.add(bvId);
                            }
                            break;
                        }
                    }
                }
                //逐个T节点轮询完之后，判断如果电压种类等于1种
                if (bvIdKindList.size()== 1){
                    System.out.println("T接线，电压种类只有一个,后面单独处理");
                }else if (bvIdKindList.size() == 2){
                    //判断如果电压种类大于1种，则直接判断父子关系
                    System.out.println("T接线，电压种类有两个");
                    processTNodeWith2KindBvId(bvIdKindList, substationWithSegments);
                }else {
                    System.out.println("活久见一个T接线，大于两种电压");
                }
            }
        }
    }
    
    //构建T接线中电压等级全部相同的情况, 由于要判断进出线，所以要单独放置在进出线常规处理完之后
    public void genTreeForTNodeSameBvId() throws SQLException{
        for (ACLineWithSubstation acLineWithSubstation: acLineWithSubstationList){
            String aclineId = acLineWithSubstation.getAclineId();


            List<SubstationWithSegment> substationWithSegments = acLineWithSubstation.getSubstationWithSegments();
            if (null != substationWithSegments && substationWithSegments.size() > 1){
                System.out.println("T接线 acline_id ："+aclineId);
                //记录此T接线的电压等级种类，如果有不同类型的，则可以直接确定父子关系
                List<String> bvIdKindList = new ArrayList<>();
                for(SubstationWithSegment substationWithSegment: substationWithSegments){
                    String stIdInT = substationWithSegment.getId();
                    //因为之前线段表的集合中是从线端过来，已经去除了虚拟站，所以加入到此集合中的stId应该都是常规站
                    //根据stId 遍历站，获取bvId， 如果其中有某一个比其他电压高，则其为父，其余为子
                    for(Substation sub: substationList){
                        String stIdInSub = sub.getId();
                        if (stIdInT.equals(stIdInSub)){
                            String bvId = sub.getBvId();
                            String name = sub.getName();
                            substationWithSegment.setBvId(bvId);
                            substationWithSegment.setName(name);
                            System.out.println("T接线 name="+ name +" bvId ："+ bvId);
                            if (!bvIdKindList.contains(bvId)){
                                bvIdKindList.add(bvId);
                            }
                            break;
                        }
                    }
                }
                //逐个T节点轮询完之后，判断如果电压种类等于1种
                if (bvIdKindList.size()== 1){
                    System.out.println("T接线，电压种类只有一个 aclineId="+aclineId+" 没经过过滤的站个数："+substationWithSegments.size());
                    //记录这个电压等级相等T的电压等级
                    String bvIdForT = bvIdKindList.get(0);
                    //T接线电压等级相等逻辑：
                    //2.1、如果没有进线，则T连接线为进线；
                    // TODO: 找出有进线且与母线相连的那个，然后找出其真正的父站
                    //找出来记录首站，一般只有一个，即有进线且有母线连接，从他开始找真正的父站
                    TransformerSubstation istNode = null;
                    //先遍历一遍找到有进线，母线一条的那种，将这个站作为首站，其他站分别作为末站，根据首站找父站，最后也建立虚拟线
                    List<TransformerSubstation> withOutInline = new ArrayList<TransformerSubstation>();
                    List<TransformerSubstation> withInline = new ArrayList<TransformerSubstation>();
                    List<TransformerSubstation> withInlineAndOneBus = new ArrayList<TransformerSubstation>();
                    // TODO:将电压等级相等的T接线站集合分成三类：无进线的、有进线的、有进线且为一条母线状态的
                    for(SubstationWithSegment substationWithSegment: substationWithSegments){
                        String stIdInT = substationWithSegment.getId();
                        String stationName = substationWithSegment.getName();
                        System.out.println("T接线电压等级相等stationName="+stationName);
                        //有些站的stId虽然在T集合中，但是实际可能是没有主变运行已经被过滤掉的
                        for(TransformerSubstation transformerSubstation: transformerSubstationList){
                        	String stId  = transformerSubstation.getStId();
                        	String transName = transformerSubstation.getName();
                        	if (stIdInT.equals(stId)){
                        		// TODO: DEBUG 数据缺失调试
                        		if (!NodeInTree.contains(transformerSubstation)) {
                        			NodeInTree.add(transformerSubstation);
								}
                        		List<InOutLines> inlines = transformerSubstation.getInLines();
                        		if (null == inlines || inlines.size()==0) {
                        			System.out.println("T接线电压等级相等，无进线的站："+transName);
                        			withOutInline.add(transformerSubstation);
								}else if (null != inlines && inlines.size()>0) {
									System.out.println("T接线电压等级相等，有进线的站："+transName+" 条数："+inlines.size());
									withInline.add(transformerSubstation);
								}
							}
                        }
                    }
                    
                    //看看各个分类里面的个数
                    int withOutInlineCount = withOutInline.size();
                    int withInlineCount = withInline.size();
//                    int withInlineAndOneBusCount = withInlineAndOneBus.size();
                    System.out.println("无进线站个数："+withOutInlineCount+" 有进线站个数："+withInlineCount);
                    //只需要区分有无进线就行
                    if ((withInlineCount == 1)) {
                    	istNode = withInline.get(0);
	                    for(TransformerSubstation transformerSubstation: withOutInline) {
	                		TransformerSubstation jstNode = transformerSubstation;
	                		String istNodeName = istNode.getName();
	                		String jstNodeName = jstNode.getName();
	                		System.out.println("T接线电压等级相等构建首末站  istNodeName="+istNodeName
	                				+" jstNodeName="+jstNodeName);
	                		//setVirtualInOutLine(istNode, jstNode);
	                		setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, null, aclineId, bvIdForT);
	                	}
					}else if(withInlineCount > 1) {
						int foundIstNodeCount = 0;
						for(TransformerSubstation transformerSubstation: withInline) {
							boolean result = confirmIstNodeForBothHasInline(transformerSubstation, null, aclineId, bvIdForT);
							if (result) {
								String stationName = transformerSubstation.getName();
								istNode = transformerSubstation;
								System.out.println("T接线电压等级相等多个站有进线，找到首站stationName="+stationName);
								foundIstNodeCount++;
							}
						}
						
	                   	if (foundIstNodeCount>1) {
                    		System.out.println("T接线电压等级相等找到多个首站，丢弃数据！！！ foundIstNodeCount="+foundIstNodeCount);
						}
					
						//如果找到首站，则遍历T
						if (null != istNode && foundIstNodeCount==1) {
							//有进线的
							for(TransformerSubstation transformerSubstation: withInline) {
	                    		TransformerSubstation jstNode = transformerSubstation;
	                    		String istNodeName = istNode.getName();
	                    		String jstNodeName = jstNode.getName();
	                    		System.out.println("T接线电压等级相等都有进线构建首末站  istNodeName="+istNodeName
	                    				+" jstNodeName="+jstNodeName);
	                    		//setVirtualInOutLine(istNode, jstNode);
	                    		setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, null, aclineId, bvIdForT);
							}
							
	                    	//无进线作为末站
	                    	for(TransformerSubstation transformerSubstation: withOutInline) {
	                    		TransformerSubstation jstNode = transformerSubstation;
	                    		String istNodeName = istNode.getName();
	                    		String jstNodeName = jstNode.getName();
	                    		System.out.println("T接线电压等级相等首站有进线末站无进线构建首末站  istNodeName="+istNodeName
	                    				+" jstNodeName="+jstNodeName);
	                    		//setVirtualInOutLine(istNode, jstNode);
	                    		setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, null, aclineId, bvIdForT);
	                    	}
	                    	
						}
					}
                    
                }else if (bvIdKindList.size() > 2) {
                    System.out.println("活久见一个T接线，大于两种电压");
                }
            }
        }
    }
    
    
    
    /**
     * 判断站点是否有进线
     * @param node
     * @return
     */
    private boolean nodeHasInline(TransformerSubstation node) {
    	List<InOutLines> inlines = node.getInLines();
    	if (null!= inlines) {
			if (inlines.size() > 0) {
				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
    }
    
    
    /**
     * #只处理确认了的首末站， 包括T里面电压相等的情况#
     * 用来设置通过首站找到的父站和末站之间的虚拟进出线
     * 	 找到父站，和末站构建虚拟进出线，并且确认两端的主变，首站的进线，即父站的出现带有哪些主变，则虚拟进出线中也是这些,
		 所以此逻辑要放在常规进出线和主变的逻辑找完之后运行
     * @param istNode  首站 
     * @param sonNode  末站
     * @return
     */
    private void setVirtualInOutLine(TransformerSubstation istNode, TransformerSubstation jstNode) {
    	
    	//首末站都无进线
    	if (null == istNode  &&  null== jstNode) {
			return;
		}
    	//两个相同的站
    	if ((null != istNode) && (null != jstNode)) {
    		String istId = istNode.getStId();
    		String jstId = jstNode.getStId();
    		if (istId.equals(jstId)) {
        		System.out.println("setVirtualInOutLine 传入两个站相同，直接返回，不处理");
    			return;
			}
		}
    	//构建虚拟线时取的线段集合是要包含虚拟线的
    	List<InOutLines> inlines = istNode.getInLines();
    	//真正的父站
    	TransformerSubstation parentSubstation = null;
    	
    	//首末站一个有进线，一个无进线
    	if (null!= inlines) {
    		// TODO:DEBUG 调试数据不全 ，此处多条进线也当一条线处理
			if (inlines.size() == 1) {
				System.out.println("首站一条进线 ");
				String parentStId = inlines.get(0).getParentStId();
				//找出子是否还有其他进线，如果没有，则此虚拟线作为唯一进线，带所有运行主变； 
				//如果有其他进线，则找出连接首末站的线段作为进线连接了末站的哪条母线，进而确认连接哪些主变。
				String sonStId = jstNode.getStId();

				String parentBvId = null;
				String sonBvId = null;
				
				List<TransformerSubstation> children = null;
				List<InOutLines> parentOutLines = null;
				List<TransformerWinding> parentTransformerWindings = null;
				for(TransformerSubstation transformerSubstation: transformerSubstationList) {
					String stId = transformerSubstation.getStId();
					if (parentStId.equals(stId)) {
						parentSubstation = transformerSubstation;
						parentBvId = transformerSubstation.getBvId();
						children = transformerSubstation.getChildren();
						parentOutLines = transformerSubstation.getOutLines();
						//找出父连接的主变，即找出父与首站连接的那条线段，即首站的进线segId或者aclineId相同的那条
						InOutLines istInline = inlines.get(0);
						String segId = istInline.getAclnSegId();
						String aclineId = istInline.getAclineId();
						
						boolean foundSeg = false;
						for(InOutLines inOutLines: parentOutLines) {
							String segIdOutLine = inOutLines.getAclnSegId();
							String aclineIdOutLine = inOutLines.getAclineId();
							
							if (null!= segId && segId.equals(segIdOutLine)) {
								parentTransformerWindings = inOutLines.getTransformerWindings();
								foundSeg = true;
								break;
							}
							
							if (null!= aclineId && aclineId.equals(aclineIdOutLine)) {
								parentTransformerWindings = inOutLines.getTransformerWindings();
								foundSeg = true;
								break;
							}
						}
						
						if (!foundSeg) {
							System.out.println("通过首站进线没有找到父节点的出线！！！！");
						}
						
					}
				}

				//末站作为子站，进线连接的主变
				List<TransformerWinding> sonTransformerWindings = new ArrayList<TransformerWinding>();
				List<InOutLines> sonInlines = null;
				for(TransformerSubstation transformerSubstation: transformerSubstationList) {
					String stId = transformerSubstation.getStId();
					if (sonStId.equals(stId)) {
						sonBvId = transformerSubstation.getBvId();
						sonInlines = transformerSubstation.getInLines();
						//无进线，则找出所有运行时主变
						if (null == sonInlines || sonInlines.size()==0) {
							System.out.println("末站无其他进线");
							List<TransformerWindingRelateBus> transformerWindingRelateBuses = transformerSubstation.getTransformerWindingRelateBuses();
							if (null != transformerWindingRelateBuses && transformerWindingRelateBuses.size()>0) {
								for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses) {
									sonTransformerWindings.add(transformerWindingRelateBus.getTransformerWinding());
								}
							}
						}else if (sonInlines.size()>0) {
							// TODO:有其他进线，则要找出当前线段连接的母线，进而确认主变
							System.out.println("末站还有其他进线");
							// TODO:偷懒逻辑，暂时默认供带子的所有运行时主变
							List<TransformerWindingRelateBus> transformerWindingRelateBuses = transformerSubstation.getTransformerWindingRelateBuses();
							if (null != transformerWindingRelateBuses && transformerWindingRelateBuses.size()>0) {
								for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBuses) {
									sonTransformerWindings.add(transformerWindingRelateBus.getTransformerWinding());
								}
							}
						}
					}
				}
				
				//子站进线, 先设置好子站，因为后面要添加到父站中作为子节点
				InOutLines inline = new InOutLines();
				inline.setParentStId(parentStId);
				inline.setSonStId(sonStId);
				inline.setVirtualLine(true);
				inline.setBvId(sonBvId);
				inline.setTransformerWindings(sonTransformerWindings);
				// TODO: 作为子站，虚拟进线中加入istNode 和 jstNode 之间的segId和 jstNode 的stId找到的bayId， 
				//方便后面子站作为T里面电压相等时的进线侧实验找母线，例如郭亮和宝雍中的靖郭线。即虚拟线要有对应的真实线映射记录。
				if (null == sonInlines) {
					sonInlines = new ArrayList<InOutLines>();
					sonInlines.add(inline);
				}else {
					sonInlines.add(inline);
				}
				
				//父站出线
				InOutLines outline = new InOutLines();
				outline.setParentStId(parentStId);
				outline.setSonStId(sonStId);
				outline.setVirtualLine(true);
				outline.setBvId(parentBvId);
				outline.setTransformerWindings(parentTransformerWindings);
				
				if (null != parentSubstation) {
					if (null == parentOutLines) {
						parentOutLines = new ArrayList<InOutLines>();
						parentOutLines.add(outline);
					}else {
						parentOutLines.add(outline);
					}
					
					
					if (null == children) {
						children = new ArrayList<TransformerSubstation>();
						children.add(jstNode);
					}else {
						children.add(jstNode);
					}
					System.out.println("setVirtualInOutLine 构建了一对真父子 parent="+parentSubstation.getName()
					+" son="+jstNode.getName());
            		// TODO: DEBUG 数据缺失调试, 后续修正可能需要关注是否真正关联到220的父站中了
            		if (!NodeInTree.contains(parentSubstation)) {
            			NodeInTree.add(parentSubstation);
					}
            		if (!NodeInTree.contains(jstNode)) {
            			NodeInTree.add(jstNode);
					}
                    System.out.println("虚拟进出线父站添加出线 parentName="+parentSubstation.getName()+
                    		" 子站添加进线 sonName= "+jstNode.getName());
				}
				
				
			}else if (inlines.size()>1) {
				System.out.println("首站多条进线");
				// TODO: 找到和母线连接的那条，确认父站
			}else {
				System.out.println("首站0条进线");
			}
		}
    }
    
    //-------------------------------将电压等级相等逻辑放在前面处理 start ------------------------------------------------//
    
    // 此处是首末站确认后要通过首站找父站，以及末站各自的母线关联的主变，最后设置到虚拟进出线中
    
    /**
     *  为了方便调用处理首站有多条进线的情况
     * @param istNode
     * @param jstNode
     * @param segIdWithjstNode
     * @param aclineIdWithjstNode
     * @param bvId
     * @throws SQLException
     */
   private void setVirtualInOutLineByConfirmedIstNode(TransformerSubstation istNode, TransformerSubstation jstNode,
   		String segIdWithjstNode, String aclineIdWithjstNode,
   		String bvId) throws SQLException{
	   List<InOutLines> inlines = istNode.getInLines();
	   if (null != inlines && inlines.size()>0) {
		   for(InOutLines inline: inlines) {
			   setVirtualInOutLineMoveBefore(istNode, jstNode, segIdWithjstNode, aclineIdWithjstNode, bvId, inline);
		   }
	   }
   }
    /**
     * 
     * @param istNode  确认后的首站
     * @param jstNode  确认后的末站
     * @param segIdWithjstNode  首末站相连的线段
     * @param aclineIdWithjstNode T里面首末站相连的T线段
     * @param bvId 某侧电压
     * @param inlineIst 首站的某一个进行，如果有多条，需要找多次
     * @throws SQLException
     */
    private void setVirtualInOutLineMoveBefore(TransformerSubstation istNode, TransformerSubstation jstNode,
    		String segIdWithjstNode, String aclineIdWithjstNode,
    		String bvId, InOutLines inlineIst) throws SQLException{
    	//首末站都无进线
    	if (null == istNode  &&  null== jstNode) {
			return;
		}
    	//两个相同的站
    	if ((null != istNode) && (null != jstNode)) {
    		String istId = istNode.getStId();
    		String jstId = jstNode.getStId();
    		if (istId.equals(jstId)) {
        		System.out.println("setVirtualInOutLineMoveBefore 传入两个站相同，直接返回，不处理");
    			return;
			}
		}

    	List<InOutLines> inlinesForIstNode = istNode.getInLines();
    	System.out.println("setVirtualInOutLineMoveBefore 电压等级相等首站进线条数："+inlinesForIstNode.size());
    	//真正的父站
    	TransformerSubstation parentSubstation = null;
		String parentStId = inlineIst.getParentStId();
		//找父站出线，即首站进线连接的父站的出侧母线
		String segId = inlineIst.getAclnSegId();
		String aclineId = inlineIst.getAclineId();
		
		//记录虚拟父站最后设置的虚拟出线连接的主变
		List<TransformerWinding> parentTransformerWindings = new ArrayList<TransformerWinding>();
		//找虚拟父站的主变和母线，因为现在普通的还没构建，只能自己找
		for(TransformerSubstation transformerSubstation: transformerSubstationList) {
			String stId = transformerSubstation.getStId();
			String parentName = transformerSubstation.getName();
			if (stId.equals(parentStId)) {
				parentSubstation = transformerSubstation;
				processPowerToBusForTransformerSubstation(transformerSubstation);
				List<TransformerWindingRelateBus> transformerWindingRelateBuses = transformerSubstation.getTransformerWindingRelateBuses();
				//找出同传入的bvId侧的母线状态
				for(TransformerWindingRelateBus windingRelateBus: transformerWindingRelateBuses) {
					String bvIdWinding = windingRelateBus.getTransformerWinding().getBvId();
					TransformerWinding winding = windingRelateBus.getTransformerWinding();
					if (bvId.equals(bvIdWinding)) {
						boolean isOneBus = windingRelateBus.getBusbarsectionStatus().isEqualOneBus();
						//为一条状态，则直接加入该侧主变到父站的供方
						if (isOneBus) {
							System.out.println("setVirtualInOutLineMoveBefore 虚拟父站一条母线状态parentName="+parentName
									+" bvId="+bvId);
							parentTransformerWindings.add(winding);
						}else {
							//该侧原始母线集合
							List<Busbarsection> orignalBuses = windingRelateBus.getBusbarsectionStatus().getOrignalBuses();
							//该侧主变连接的母线
							List<Busbarsection> windingBuses = windingRelateBus.getBusbarsectionStatus().getBusbarsections();
							//虚拟父站出线侧连接的母线集合
							List<Busbarsection> outlineBuses = findBusBySegOrAclineId(orignalBuses, segId, aclineId, parentStId);
							if (null != windingBuses && null != outlineBuses) {
								for(Busbarsection windingBus: windingBuses) {
									for(Busbarsection inlineBus: outlineBuses) {
										if (windingBus.equals(inlineBus)) {
											//该侧主变找到和出线有相连接的母线
											System.out.println("虚拟父站的主变找到和出线连接的母线parentName="+parentName);
											parentTransformerWindings.add(winding);
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		//处理虚拟子站
		String sonStId = jstNode.getStId();
		String sonName = jstNode.getName();
		//记录虚拟子站最后设置的虚拟进线连接的主变
		List<TransformerWinding> sonTransformerWindings = new ArrayList<TransformerWinding>();
		processPowerToBusForTransformerSubstation(jstNode);
		List<TransformerWindingRelateBus> transformerWindingRelateBuses = jstNode.getTransformerWindingRelateBuses();
		//找出同传入的bvId侧的母线状态
		for(TransformerWindingRelateBus windingRelateBus: transformerWindingRelateBuses) {
			String bvIdWinding = windingRelateBus.getTransformerWinding().getBvId();
			TransformerWinding winding = windingRelateBus.getTransformerWinding();
			if (bvId.equals(bvIdWinding)) {
				boolean isOneBus = windingRelateBus.getBusbarsectionStatus().isEqualOneBus();
				//为一条状态，则直接加入该侧主变到父站的供方
				if (isOneBus) {
					System.out.println("setVirtualInOutLineMoveBefore 虚拟子站一条母线状态sonName="+sonName
							+" bvId="+bvId);
					sonTransformerWindings.add(winding);
				}else {
					//该侧原始母线集合
					List<Busbarsection> orignalBuses = windingRelateBus.getBusbarsectionStatus().getOrignalBuses();
					//该侧主变连接的母线
					List<Busbarsection> windingBuses = windingRelateBus.getBusbarsectionStatus().getBusbarsections();
					//虚拟子站进线侧连接的母线集合
					List<Busbarsection> inlineBuses = findBusBySegOrAclineId(orignalBuses, segIdWithjstNode, aclineIdWithjstNode, sonStId);
					if (null != windingBuses && null != inlineBuses) {
						for(Busbarsection windingBus: windingBuses) {
							for(Busbarsection inlineBus: inlineBuses) {
								if (windingBus.equals(inlineBus)) {
									//该侧主变找到和出线有相连接的母线
									System.out.println("虚拟子站的主变找到和进线连接的母线 sonName="+sonName);
									sonTransformerWindings.add(winding);
									break;
								}
							}
						}
					}
				}
			}
		}
		
		
		//分别给虚拟父子站点设置进出线
		List<InOutLines> parentOutLines = parentSubstation.getOutLines();
		List<InOutLines> sonInlines = jstNode.getInLines();
		List<TransformerSubstation> children = parentSubstation.getChildren();
		//子站进线, 先设置好子站，因为后面要添加到父站中作为子节点
		InOutLines inline = new InOutLines();
		inline.setParentStId(parentStId);
		inline.setSonStId(sonStId);
		inline.setVirtualLine(true);
		inline.setBvId(bvId);
		inline.setTransformerWindings(sonTransformerWindings);
		//加入原来的进线集合中
		if (null == sonInlines) {
			sonInlines = new ArrayList<InOutLines>();
			sonInlines.add(inline);
		}else {
			sonInlines.add(inline);
		}
		
		//父站出线
		InOutLines outline = new InOutLines();
		outline.setParentStId(parentStId);
		outline.setSonStId(sonStId);
		outline.setVirtualLine(true);
		outline.setBvId(bvId);
		outline.setTransformerWindings(parentTransformerWindings);
		
		if (null != parentSubstation) {
			if (null == parentOutLines) {
				parentOutLines = new ArrayList<InOutLines>();
				parentOutLines.add(outline);
			}else {
				parentOutLines.add(outline);
			}
			
			
			if (null == children) {
				children = new ArrayList<TransformerSubstation>();
				children.add(jstNode);
			}else {
				children.add(jstNode);
			}
			System.out.println("setVirtualInOutLineMoveBefore 构建了一对真父子 parent="+parentSubstation.getName()
			+" son="+jstNode.getName());
			
    		// TODO: DEBUG 数据缺失调试, 后续修正可能需要关注是否真正关联到220的父站中了
    		if (!NodeInTree.contains(parentSubstation)) {
    			NodeInTree.add(parentSubstation);
			}
    		if (!NodeInTree.contains(jstNode)) {
    			NodeInTree.add(jstNode);
			}
            System.out.println("虚拟进出线父站添加出线 parentName="+parentSubstation.getName()+
            		" 子站添加进线 sonName= "+jstNode.getName());
		}
		
    }
    
    // TODO: 是否要加标记，标识该站已经找过主变和母线的关系了。
    //将主变和母线关系单独成一个个站处理，方便电压等级相等情况的复用
    //此处不能用进出线条数简单逻辑处理，因为电压等级相等的情况还没有构建出进出线
    /**
     *  不根据进出线条数来遍历某个站高低进出侧的主变和母线关联关系
     * @param transformerSubstation
     * @throws SQLException
     */
    private void processPowerToBusForTransformerSubstation(TransformerSubstation transformerSubstation) throws  SQLException{
    	
    	//如果该站已经找过，就不要再重复找，添加进来
    	if (transformerSubstation.isHasFoundPowerToBus()) {
			return;
		}
        //各个站下面的运行时主变
        List<PowerTransformerWithWinding> runningTransformers = transformerSubstation.getRunningTransformers();
        String bvIdSub = transformerSubstation.getBvId();
        //主变母线关系集合
        List<TransformerWindingRelateBus> transformerWindingRelateBusList = transformerSubstation.getTransformerWindingRelateBuses();
        if (null == transformerWindingRelateBusList){
            transformerWindingRelateBusList = new ArrayList<>();
        }
        // 220kv 的站找中低压侧为110或者35的，其余不管
        // 110kv 的找高压侧输入的以及中低压输出为35的，其余不管
        // 35KV 的找高压侧输入的，其余不管
        String stationName = transformerSubstation.getName();
        System.out.println("processPowerToBusForTransformerSubstation stationName="+stationName
        		+" bvIdSub="+bvIdSub);

        // 输出侧到母线，包括220和110
        if((Constants.BV_ID_220.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub))){
            for (PowerTransformerWithWinding powerTransformerWithWinding : runningTransformers) {
                //各个主变各侧的绕阻
                List<TransformerWinding> windings = powerTransformerWithWinding.getWindings();
                for (TransformerWinding transformerWinding : windings) {
                    String windType = transformerWinding.getWindType();
                    String bvIdWinding = transformerWinding.getBvId();
                    String yxClose = transformerWinding.getYx_close();
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //中低压侧且为110或者35KV的，因为还会出现6KV这种
                        if ((Constants.WINDING_MIDDLE.equals(windType) || Constants.WINDING_LOW.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                            // 查找主变和母线的关系
                            BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
                            busbarsectionStatus = findPowerToBusRelation(transformerWinding);
   
                            TransformerWindingRelateBus transformerWindingRelateBus = new TransformerWindingRelateBus();
                            transformerWindingRelateBus.setBusbarsectionStatus(busbarsectionStatus);
                            transformerWindingRelateBus.setTransformerWinding(transformerWinding);
                            transformerWindingRelateBusList.add(transformerWindingRelateBus);
                        }
                    }
                }
            }
        }

        //输入侧到母线，包括110和35
        if((Constants.BV_ID_35.equals(bvIdSub) || Constants.BV_ID_110.equals(bvIdSub))){
            for (PowerTransformerWithWinding powerTransformerWithWinding : runningTransformers) {
                //各个主变各侧的绕阻
                List<TransformerWinding> windings = powerTransformerWithWinding.getWindings();
                for (TransformerWinding transformerWinding : windings) {
                    String windType = transformerWinding.getWindType();
                    String bvIdWinding = transformerWinding.getBvId();
                    String yxClose = transformerWinding.getYx_close();
                    //遥信闭合的
                    if (Constants.WINDING_YX_CLOSE.equals(yxClose)) {
                        //高压输入侧，电压等级类型和输出侧一直，因为出入都是相对的。
                        if ((Constants.WINDING_HIGH.equals(windType)) &&
                                (Constants.BV_ID_110.equals(bvIdWinding)||Constants.BV_ID_35.equals(bvIdWinding))) {
                            // 查找主变和母线的关系
                            BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
                            busbarsectionStatus = findPowerToBusRelation(transformerWinding);
      
                            TransformerWindingRelateBus transformerWindingRelateBus = new TransformerWindingRelateBus();
                            transformerWindingRelateBus.setBusbarsectionStatus(busbarsectionStatus);
                            transformerWindingRelateBus.setTransformerWinding(transformerWinding);
                            transformerWindingRelateBusList.add(transformerWindingRelateBus);
                        }
                    }
                }
            }
        }

        //设置站主变和母线关系集合
        transformerSubstation.setTransformerWindingRelateBuses(transformerWindingRelateBusList);
        transformerSubstation.setHasFoundPowerToBus(true);
        // TODO:DEBUG 调试数据不全
        for(TransformerWindingRelateBus transformerWindingRelateBus: transformerWindingRelateBusList) {
        	BusbarsectionStatus busbarsectionStatus = transformerWindingRelateBus.getBusbarsectionStatus();
        	TransformerWinding winding = transformerWindingRelateBus.getTransformerWinding();
        	String windingName = null;
        	if (null != winding) {
        		windingName = winding.getName();
			}
        	//如果不是一条母线状态，而且没有母线，则输出
        	if (null != busbarsectionStatus){          		
        		if (!busbarsectionStatus.isEqualOneBus()) {
					List<Busbarsection> busbarsections = busbarsectionStatus.getBusbarsections();
					if (null == busbarsections || busbarsections.size() ==0) {
						System.out.println("主变没有找到一条母线 windingName="+windingName);
					}
				}
			}else {
				System.out.println("主变没有找到一条母线 windingName="+windingName);
			}
        }
    }
    
    
    /**
     *   通过线段找连接的母线
     * @param busbarsections 原始母线集合
     * @param segId  线段id
     * @param aclineId T接线id
     * @param stId 站ID
     * @return
     * @throws SQLException
     */
    private List<Busbarsection> findBusBySegOrAclineId(List<Busbarsection> busbarsections, String segId,
    		String aclineId, String stId) throws  SQLException{
        //记录与进出现相连的母线记录
        List<Busbarsection> busWithInOutLine = new ArrayList<>();

        String bayId = null;
        if (null != segId){
           bayId = findBayIdBySegIdAndStId(segId, stId);
        }
        
        if (null != aclineId) {
        	bayId = findBayIdByAclineIdAndStId(aclineId, stId);
		}
        
        if (null != bayId){
            List<Disconnector> disconnectors = findDisconnectorByBayId(bayId);
            if (null !=disconnectors && disconnectors.size() >0){
                //遍历母线里面的ND和查到的隔刀的ind或者jnd，如果有相等的，则查遥信
                //记录闭合的母线
                for(Busbarsection busbarsection: busbarsections){
                    String nd = busbarsection.getNd();
                    for(Disconnector disconnector: disconnectors){
                        String ind = disconnector.getInd();
                        String jnd = disconnector.getJnd();
                        if(nd.equals(ind) || nd.equals(jnd)){
                            String disconnetorId = disconnector.getId();
                            boolean result = YXStatusEqualMoment(queryMoment,
                                    disconnetorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
                            if (result){
                                //记录连接闭合的母线
                                busWithInOutLine.add(busbarsection);
                                System.out.println("findBusBySegOrAclineId 记录母线busbarsection name="+busbarsection.getName());
                            }
                        }
                    }
                }
            }else {
                System.out.println("findBusBySegOrAclineId 通过bayId未找到出线所在间隔的隔刀bayId="+bayId);
                // TODO: 通过End的 ND 继续找隔刀
            }
        }else{
            System.out.println("#####findBusBySegOrAclineId 通过segId和stId未找到间隔");
        }
        
        return busWithInOutLine;
    }
    
    

    /**
     * 确认电压等级相等的两个站的首末站情况，在都有进线的情况下
     * @param istNode 测试首站
     * @param jstNode  测试末站
     * @param exchanged 是否交换
     * @param segId  线段ID
     * @param aclineId T接线ID
     * @param bvId 某侧电压
     * @throws SQLException
     */
    private boolean confirmIstNodeForBothHasInline(TransformerSubstation istNode, 
    		String segId, String aclineId,
    		String bvId)  throws  SQLException{

    	//查找高压侧母线状态
    	List<PowerTransformerWithWinding> runningTransformers = istNode.getRunningTransformers();
    	TransformerWinding transformerWinding = null;
    	String bvIdWinding = null;
    	String isVariantGroup = null;
    	BusbarsectionStatus busbarsectionStatus = null;
    	if (null != runningTransformers && runningTransformers.size()>0) {
    		List<TransformerWinding> windings = runningTransformers.get(0).getWindings();
    		for(TransformerWinding winding: windings) {
    			String windType = winding.getWindType();
				transformerWinding = winding;
				bvIdWinding = winding.getBvId();
				isVariantGroup = winding.getIsLineVariantGroup();
			
    			if (/*Constants.WINDING_HIGH.equals(windType) &&*/
    					(bvIdWinding.equals(bvId))) {
    				busbarsectionStatus = findPowerToBusRelation(transformerWinding);
    				break;
				}
    		}
		}
    	
    	if (null != busbarsectionStatus) {
    		if (busbarsectionStatus.isEqualOneBus()) {
    			//首站确认
        		//setVirtualInOutLine(istNode, jstNode);
//    			List<InOutLines> inlines = istNode.getInLines();
//    			if (null != inlines && inlines.size()>0) {
//    				for(InOutLines inline: inlines) {
//        	        	setVirtualInOutLineMoveBefore(istNode, jstNode, segId, aclineId, bvId, inline);
//    				}
//				}
    			System.out.println("确认首站中为一条母线状态");
    			return true;

			}else {
				//确认线段和其他进线是否连接到同一条母线
				List<Busbarsection> originalBuses = busbarsectionStatus.getOrignalBuses();
		    	//对于母线不是一条状态，需要最后和进线对比，是否连上了同一条母线
		    	List<InOutLines> inlines = istNode.getInLines();
		    	//首站id
		    	String istNodeStId = istNode.getStId();
		    	//先通过segId或者aclineId找到实验首站的母线集合
		    	List<Busbarsection> istNodeBuses = findBusBySegOrAclineId(originalBuses, segId, null, istNodeStId);
		    	boolean istNodeConfirmed = false;
		    	//多条进线逐条处理，看是否有相连
		    	for(InOutLines inline: inlines) {
		    		String inlineSegId = inline.getAclnSegId();
		    		String inlineAclineId = inline.getAclineId();
		    		List<Busbarsection> inlineBuses = null;
		    		System.out.println("进线inlineSegId="+inlineSegId+" inlineAclineId="+inlineAclineId);
		    		if (null != inlineSegId) {
		    			inlineBuses = findBusBySegOrAclineId(originalBuses, inlineSegId, null, istNodeStId);
					}else if(null != inlineAclineId){
						inlineBuses = findBusBySegOrAclineId(originalBuses, null, inlineAclineId, istNodeStId);
					}else {
						System.out.println("进线中没有seg或者aclineId 记录");
					}
		    		
		    		//遍历是否有连接同一条母线
		    		if (null != istNodeBuses && null != inlineBuses) {
						for(Busbarsection istNodeBusbarsection: istNodeBuses) {
							for(Busbarsection inlineBusbarsection: inlineBuses) {
								//连接上了
								if (istNodeBusbarsection.equals(inlineBusbarsection)) {
									istNodeConfirmed = true;
									//setVirtualInOutLine(istNode, jstNode);
									//setVirtualInOutLineMoveBefore(istNode, jstNode, segId, aclineId, bvId, inline);
									System.out.println("进线中找到与确认线连接了相同的母线");
									return true;
								}
							}
						}
					}
		    	}
		    	
		    	//非T调用，如果遍历完多条之后，实验首站仍然没有找到连接在一起的母线，则需要换另外一个站作为首站了
//		    	if (!istNodeConfirmed && !isTCall) {
//		    		System.out.println("实验首站没找到连接母线，交换首末站");
//		    		//setVirtualInOutLine(jstNode, istNode);
//	    			List<InOutLines> jstinlines = jstNode.getInLines();
//	    			if (null != jstinlines && jstinlines.size()>0) {
//	    				for(InOutLines inline: jstinlines) {
//	        	        	setVirtualInOutLineMoveBefore(jstNode, istNode, segId, aclineId, bvId, inline);
//	    				}
//					}
//				}
			}
		}
    	
		return false;
    }
  //-------------------------------将电压等级相等逻辑放在前面处理   end------------------------------------------------//

    
    
   
    /**
     * 首末站都有进线，则随机先找一个作为首站，找其高压侧母线，然后假设该线段为其出线，看是否与其母线相连,连接则确实为首站，
       否则将另外那个站作为首站
     * @param istNode  实验认为的首站
     * @param jstNode  实验认为的末站
     * @param exchanged  用来确认是否交换首末站的递归调用
     * @param acLineEndWithSegment  首末站的线段
     * @throws SQLException
     */
    private void bothNodeHasInline(TransformerSubstation istNode, TransformerSubstation jstNode, 
    		boolean exchanged, ACLineEndWithSegment acLineEndWithSegment)  throws  SQLException{
    	//随机找一个站作为首站，找线段是否确实为其高压侧出线， 确认首末站后，后续的逻辑调用setVirtualInOutLine处理
    	System.out.println("bothNodeHasInline 是否交换首末站exchanged="+exchanged);
    	//高压侧母线
    	List<PowerTransformerWithWinding> runningTransformers = istNode.getRunningTransformers();
    	TransformerWinding transformerWinding = null;
    	String stIdPower = null;
    	String bvIdWinding = null;
    	String isVariantGroup = null;
    	if (null != runningTransformers && runningTransformers.size()>0) {
    		List<TransformerWinding> windings = runningTransformers.get(0).getWindings();
    		for(TransformerWinding winding: windings) {
    			String windType = winding.getWindType();
    			if (Constants.WINDING_HIGH.equals(windType)) {
    				transformerWinding = winding;
    				stIdPower = winding.getStId();
    				bvIdWinding = winding.getBvId();
    				isVariantGroup = winding.getIsLineVariantGroup();
				}
    		}
		}
    	
    	//对于母线不是一条状态，需要最后和进线对比，是否连上了同一条母线
    	List<InOutLines> inlines = istNode.getInLines();
    	
    	//显示线段信息
    	String segId = acLineEndWithSegment.getAclnsegId();
    	String aclineId = acLineEndWithSegment.getAclineId();
    	System.out.println("bothNodeHasInline segId="+segId
    			+" aclineId="+aclineId);

    	System.out.println("bothNodeHasInline 开始找母线");
        BusbarsectionStatus busbarsectionStatus = new BusbarsectionStatus();
        //查找主变输出侧的母线
        List<Busbarsection> busbarsections = findBusbarsectionByBvIdAndstId(bvIdWinding, stIdPower);
        //只有为多条母线，并且断路器没有全部闭合时需要返回确定主变该侧连接的具体母线集合
        List<Busbarsection> needReturnBuses = new ArrayList<>();
        if ((null != busbarsections)  && (busbarsections.size()>0)){
            if(busbarsections.size() == 1){
                //一条母线，不用找主变与母线的对应关系
                System.out.println("bothNodeHasInline 1条母线");
                busbarsectionStatus.setEqualOneBus(true);
            }else if(busbarsections.size() == 2){
                System.out.println("bothNodeHasInline 2条母线");
                //两条母线，找母联断路器
                List<Breaker> breakers = findBreakerForBusbarsection(bvIdWinding, stIdPower);
                if (null!=breakers && breakers.size()>0){
                    if(breakers.size() == 1){
                        //查遥信，确认两条母线是否相连
                        String breakerId = breakers.get(0).getId();
                        boolean result = YXStatusEqualMoment(queryMoment,
                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                        if (result){
                            System.out.println("母联断路器闭合，不用记录主变和母线编号对应关系");
                            busbarsectionStatus.setEqualOneBus(true);
                        }else {
                            System.out.println("bothNodeHasInline 母联断路器打开，查找主变对应哪条母线,找隔刀关联");
                            busbarsectionStatus.setEqualOneBus(false);
                            // TODO: 通过线段找End再找间隔，找隔刀是否与母线连接；找到母线后确认是否与该站的其他进线连接的母线为同一条
                            List<Busbarsection> temp = findBusForHighWindTypeOutLine(busbarsections, segId, stIdPower);
                            needReturnBuses.addAll(temp);
                        }
                    }else {
                        System.out.println("####活久见，两条母线查到多个母联断路器");
                    }
                }else {
                    System.out.println("####bothNodeHasInline 2条母线未找到母联断路器#####");
                    // TODO: 一个断路器都没找到，直接根据编号找隔刀： 2条母线 110KV侧，隔刀关键字：5001 和 5002，  35kv侧 4001 和 4002；
                    //  3 条母线 110KV侧，隔刀关键字：5001 和 5002  35kv 4001 和 4002 I母和 II母； 隔刀关键字：5402 和 5404  35kv 4402 和 4404 II母和IV母
                    // 再根据找到的隔刀bayId ，再找断路器， 如果找不到断路器，直接使用隔刀的遥信。
                    String disconnectorNumber = null;
                    String disconnectorBayId;
                    if(Constants.BV_ID_110.equals(bvIdWinding)) {
                    	disconnectorNumber = "5001";
                    }else if (Constants.BV_ID_35.equals(bvIdWinding)) {
                    	disconnectorNumber = "4001";
					}
                    
                	Disconnector disconnector = findDisconnetorByNumberAndBvIdStId(bvIdWinding, stIdPower, disconnectorNumber);
                	if (null != disconnector) {
                		disconnectorBayId = disconnector.getBayId();
                		System.out.println("disconnectorBayId="+disconnectorBayId);
                		Breaker breaker = findBreakerByBayId(disconnectorBayId);
                		if (null != breaker) {
							String breakerId = breaker.getId();
							System.out.println("通过隔刀编号关键字找到间隔，并找到断路器breakerId="+breakerId+" bvIdWinding="+bvIdWinding);
	                        boolean result = YXStatusEqualMoment(queryMoment,
	                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
	                        if (result){
	                            System.out.println("通过隔刀编号关键字找到间隔母联断路器闭合，不用记录主变和母线编号对应关系");
	                            busbarsectionStatus.setEqualOneBus(true);
	                        }else {
	                            System.out.println("bothNodeHasInline 母联断路器打开，查找主变对应哪条母线,找隔刀关联");
	                            busbarsectionStatus.setEqualOneBus(false);
	                            // TODO: 通过线段找End再找间隔，找隔刀是否与母线连接；找到母线后确认是否与该站的其他进线连接的母线为同一条
	                            List<Busbarsection> temp = findBusForHighWindTypeOutLine(busbarsections, segId, stIdPower);
	                            needReturnBuses.addAll(temp);
	                        }
						}
					}else {
						System.out.println("通过隔刀编号关键字找不到隔刀");
						// TODO: DEBUG 确实数据调试，默认一条母线
						busbarsectionStatus.setEqualOneBus(true);
					}
                }
            }else if(busbarsections.size() == 3){
                System.out.println("bothNodeHasInline 3条母线");
                //三条母线，找母联断路器，并且确定哪个断路器和哪两条母线相连
                List<Breaker> breakers = findBreakerForBusbarsection(bvIdWinding, stIdPower);
                if (null!=breakers && breakers.size()>0) {
                    if (breakers.size() == 1){
                        String breakerId = breakers.get(0).getId();
                        boolean breakerResult = YXStatusEqualMoment(queryMoment,
                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                        if (breakerResult){
                            System.out.println("三母线只有一个断路器，闭合");
                        }else {
                            System.out.println("三母线只有一个断路器，断开");
                        }
                        //需要继续用关键字“母联”查找隔刀，判断遥信，然后与找到的断路器来确认三条母线连接状态
                        Disconnector disconnector = findDisconnetorByNameAndBvIdStId(bvIdWinding, stIdPower);
                        boolean disconnetorResult = false;
                        if (null != disconnector){
                            String disconnectorId = disconnector.getId();
                            disconnetorResult = YXStatusEqualMoment(queryMoment,
                                    disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
                        }else {
                            System.out.println("三母线只有一个断路器，隔刀未找到");
                            // TODO: 没有找到隔刀，默认为闭合处理
                            disconnetorResult = true;
                        }

                        if(breakerResult && disconnetorResult){
                            //两者都闭合，则为一条母线状态
                            busbarsectionStatus.setEqualOneBus(true);
                            System.out.println("三母一断一隔刀都闭合，三连状态");
                        }else {
                            busbarsectionStatus.setEqualOneBus(false);
                            // TODO:找到高压侧出现和哪条母线连接
                            List<Busbarsection> temp = findBusForHighWindTypeOutLine(busbarsections, segId, stIdPower);
                            needReturnBuses.addAll(temp);
                        }
                    }else if (breakers.size() == 2) {
                        //查遥信，确认三条条母线是否相连
                        String breaker1Id = breakers.get(0).getId();
                        String breaker2Id = breakers.get(1).getId();
                        boolean result1 = YXStatusEqualMoment(queryMoment,
                                breaker1Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result2 = YXStatusEqualMoment(queryMoment,
                                breaker2Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        if (result1 && result2) {
                            System.out.println("母联断路器都闭合，不用记录主变和母线编号对应关系");
                            busbarsectionStatus.setEqualOneBus(true);
                        }else {
                            busbarsectionStatus.setEqualOneBus(false);
                            // TODO:找到高压侧出现和哪条母线连接
                            List<Busbarsection> temp = findBusForHighWindTypeOutLine(busbarsections, segId, stIdPower);
                            needReturnBuses.addAll(temp);
                        }
                    } else if(breakers.size() == 3){
                        //查遥信，确认三条条母线是否相连
                        String breaker1Id = breakers.get(0).getId();
                        String breaker2Id = breakers.get(1).getId();
                        String breaker3Id = breakers.get(2).getId();
                        boolean result1 = YXStatusEqualMoment(queryMoment,
                                breaker1Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result2 = YXStatusEqualMoment(queryMoment,
                                breaker2Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        boolean result3 = YXStatusEqualMoment(queryMoment,
                                breaker3Id, Constants.COLUMN_ID_BREAKER, 0, true);
                        //其中任意两个闭合
                        if(result1&&result2 || result2&&result3 || result3&&result1){
                            System.out.println("母联断路器都闭合，不用记录主变和母线编号对应关系");
                            busbarsectionStatus.setEqualOneBus(true);
                        }else {
                            busbarsectionStatus.setEqualOneBus(false);
                            // TODO:找到高压侧出现和哪条母线连接
                            List<Busbarsection> temp = findBusForHighWindTypeOutLine(busbarsections, segId, stIdPower);
                            needReturnBuses.addAll(temp);
                        }
                    }else {
                        System.out.println("三条母线，断路器个数=" + breakers.size());
                    }
                }else{
                    System.out.println("#####三条母线，没有找到母联断路器");
                }
            }else if(busbarsections.size() == 4){
                System.out.println("bothNodeHasInline 4条母线");
                //同三条
            }else {
                System.out.println("####活久见找到超过4条母线#####");
            }

        }else {          
            System.out.println("####未找到主变侧母线#####  isVariantGroup="+isVariantGroup);
            //线变组逻辑在进线和主变的对应关系中已经处理
            // TODO: DEBUG 暂时默认为只有一条母线处理
            busbarsectionStatus.setEqualOneBus(true);
        }
        
        busbarsectionStatus.setBusbarsections(needReturnBuses);
        
        

    	boolean isOneBus = false;
    	int busCount = 0;
    	List<Busbarsection> findBusbarsections = null;
		if (null != busbarsectionStatus) {
			isOneBus = busbarsectionStatus.isEqualOneBus();
			findBusbarsections = busbarsectionStatus.getBusbarsections();
			busCount = (findBusbarsections==null)?0:findBusbarsections.size();
			
			if (!isOneBus) {
				System.out.println("bothNodeHasInline 非一条母线状态 busCount="+busCount);
				
			}else {
				System.out.println("bothNodeHasInline 一条母线状态");
			}
		}
		

    	
        System.out.println("bothNodeHasInline 高压侧的母线状态 isOneBus="+isOneBus+" busCount="+busCount);
        // 当有母线不为一条状态时，看首站的进线中是否有相同的母线和 高压侧出现连接的母线
        boolean foundSameBus = false;
        if (!isOneBus) {
			if (busCount >0 ) {
				// TODO:暂时都只考虑一条进线的情况，多条进线需要再去找对应的父站
				//取进线连接的母线
				InOutLines inline = null;
				List<Busbarsection> inLineBuses = null;
				if (null != inlines && inlines.size()>0) {
					inline = inlines.get(0);
					BusbarsectionStatus busbarsectionStatus2 = inline.getBusbarsectionStatus();
					if (null != busbarsectionStatus2) {
						inLineBuses = busbarsectionStatus2.getBusbarsections();
					}
				}

				if (null != inLineBuses && inLineBuses.size()>0 ) {
					for(Busbarsection busbarsection: findBusbarsections) {
						for(Busbarsection inlineBus: inLineBuses) {
							if (busbarsection.equals(inlineBus)) {
								//找到高压侧出现和测试首站进线有连接到同一条母线，设定为首站成功；
								foundSameBus = true;
								break;
							}
						}				
					}
				}
			}
		}
        
       //如果第一次的默认首站中找到了高压侧的母线但是没有找到和进线连接的， 或者首站中没有找到连接的母线
        //则需要替换另一个站为首站来尝试
        boolean NotFoundBusWithHighOutLine = (!isOneBus) && (busCount==0);
        boolean NotFoundSameBusWithInline = (!isOneBus) && (busCount >0) && (!foundSameBus);
        if ((NotFoundBusWithHighOutLine ||
        		NotFoundSameBusWithInline) && !exchanged) {
        	bothNodeHasInline(jstNode, istNode, true, acLineEndWithSegment);
		}
        
        
        if (isOneBus || foundSameBus) {
        	//首末站确定，对于都有进线的情况
        	System.out.println("两个站都有进线，最终确定首末站，isOneBus="+isOneBus+" foundSameBus="+foundSameBus);
			setVirtualInOutLine(istNode, jstNode);
		}
        
    }
    
    
    
    /**
     * //根据线段ID和stId找 高压侧出现连接的母线
     * @param busbarsections 高压侧母线集合
     * @param segId 电压相等两个站的线段ID
     * @param stId 高压侧绕阻stId
     * @return
     * @throws SQLException
     */
    private List<Busbarsection> findBusForHighWindTypeOutLine(List<Busbarsection> busbarsections, String segId,
    		String stId) throws  SQLException{
        //记录与进出现相连的母线记录
        List<Busbarsection> busWithInOutLine = new ArrayList<>();

        if (null != segId){
            //通过线段查线端，然后通过端的bayId查找间隔隔刀与母线连接情况
            String bayId = findBayIdBySegIdAndStId(segId, stId);
            if (null != bayId){
                List<Disconnector> disconnectors = findDisconnectorByBayId(bayId);
                if (null !=disconnectors && disconnectors.size() >0){
                    //遍历母线里面的ND和查到的隔刀的ind或者jnd，如果有相等的，则查遥信
                    //记录闭合的母线
                    for(Busbarsection busbarsection: busbarsections){
                        String nd = busbarsection.getNd();
                        for(Disconnector disconnector: disconnectors){
                            String ind = disconnector.getInd();
                            String jnd = disconnector.getJnd();
                            if(nd.equals(ind) || nd.equals(jnd)){
                                String disconnetorId = disconnector.getId();
                                boolean result = YXStatusEqualMoment(queryMoment,
                                        disconnetorId, Constants.COLUMN_ID_DISCONNECTOR, 0, true);
                                if (result){
                                    //记录连接闭合的母线
                                    busWithInOutLine.add(busbarsection);
                                    System.out.println("记录母线busbarsection name="+busbarsection.getName());
                                }
                            }
                        }
                    }
                }else {
                    System.out.println("findBusForHighWindTypeOutLine 通过bayId未找到出线所在间隔的隔刀bayId="+bayId);
                    // TODO: 通过End的 ND 继续找隔刀
                }
            }else{
                System.out.println("#####findBusForHighWindTypeOutLine 通过segId和stId未找到间隔");
            }
        }
        
        return busWithInOutLine;
    }
    
    //在常规构建和T构建完之后，寻找电压等级相等的 ist, jst 首末站，例如靖港和郭亮这种
    private void genSameBvIdForIstAndJstId(String bvId) throws  SQLException{
    	
    	if (null == bvId) {
			return;
		}
    	//记录找到的关系对，防止重复查找
    	List<String> recordFoundCouple = new ArrayList<String>();
    	for(TransformerSubstation node1: transformerSubstationList){
    		String bvIdNode1 = node1.getBvId();
    		String stIdNode1 = node1.getStId();
    		String node1Name = node1.getName();
    		//指定电压等级的站
    		if (bvId.equals(bvIdNode1)) {
    			//列出该站里面的线段，找出里面是否有 istid, jstid 等于两个站的情况
        		List<ACLineEndWithSegment> endWithSegmentsNode1 = node1.getEndWithSegments();
        		for(ACLineEndWithSegment acLineEndWithSegment: endWithSegmentsNode1) {
        			String istIdNode1 = acLineEndWithSegment.getIstId();
        			String jstIdNode1 = acLineEndWithSegment.getJstId();
        			
                	for(TransformerSubstation node2: transformerSubstationList){
                		String bvIdNode2 = node2.getBvId();
                		String stIdNode2 = node2.getStId();
                		String node2Name = node2.getName();
                		
            			//去除同一个站的情况
            			if (stIdNode1.equals(stIdNode2)) {
							continue;
						}
            			
                		//电压都为指定电压
                		if (bvId.equals(bvIdNode2)) {
                			if ((istIdNode1.equals(stIdNode1) && jstIdNode1.equals(stIdNode2))|| 
                					(istIdNode1.equals(stIdNode2) && jstIdNode1.equals(stIdNode1))) {
								System.out.println("符合电压等级相等而且ist jst互为对方的情况 node1Name="+node1Name+" node2Name="+node2Name
										+" bvId="+bvId);	
								//只要不同时包含双方，则需要进行构建
								if (!(recordFoundCouple.contains(stIdNode1) && recordFoundCouple.contains(stIdNode2))) {
									System.out.println("genSameBvIdForIstAndJstId node1Name="+node1Name+" node2Name="+node2Name
											+" bvId="+bvId);
									recordFoundCouple.add(stIdNode1);
									recordFoundCouple.add(stIdNode2);
									// TODO: 处理进出线和真正的父子找寻关系逻辑
								    // 找出istId=stId 的站，然后看其有几条进线，只有一条，则直接是该线段带了二个子站，将另外一个站加入父站的子节点中，并且构建虚拟进出线
									//如果在istId=stId的站中找到多条进线， 例如曹家坪带了明月， 明月又连了江背，则找出明月高压侧出线的母线和江背的进线如何连接，来确认是哪个站带的江背
									//首站
									TransformerSubstation istNode = null;
									//末站
									TransformerSubstation jstNode = null;
									
									//先找谁没有进线，则将其作为末站，另一个作为首站
									boolean node1HasInline = nodeHasInline(node1);
									boolean node2HasInline = nodeHasInline(node2);
									// TODO： 需要查明情况，都无进线，前面通过常规和T构建，应该至少有一个能有父站，即有进线
									if (!node1HasInline && !node2HasInline) {
										System.out.println("两站都无进线node1="+node1.getName()
										+" node2="+node2.getName());
										
									}else {
										//至少有一个有进线
										
										//node1 没有进线，作为末站
										if (!node1HasInline) {
											istNode = node2;
											jstNode = node1;
											//如果两个都有进线，则随机找一个，看其另外的进线是否和连接两个站的线段是否通过母线相连，如果是，则连接线段为出线，即该站为首站，
											//另外那个为末站。否则，使用另外一个站作为首站，因为不会连接两个站的线段没有与其中任何一个站的其他进线连接的情况。
											//都有进线的情况，先找进线少的那个来进行分析，如果都为多条进线，则同样找是否有通过母线连通的，有，则此线段为出线,则此站为首站，另外为末站。
											//setVirtualInOutLine(istNode, jstNode);
											setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, 
													acLineEndWithSegment.getAclnsegId(), null, bvId);
										}else {
											//node2没有进线，作为末站
											if (!node2HasInline) {
												istNode = node1;
												jstNode = node2;
												//如果两个都有进线，则随机找一个，看其另外的进线是否和连接两个站的线段是否通过母线相连，如果是，则连接线段为出线，即该站为首站，
												//另外那个为末站。否则，使用另外一个站作为首站，因为不会连接两个站的线段没有与其中任何一个站的其他进线连接的情况。
												//都有进线的情况，先找进线少的那个来进行分析，如果都为多条进线，则同样找是否有通过母线连通的，有，则此线段为出线,则此站为首站，另外为末站。
												//setVirtualInOutLine(istNode, jstNode);
												setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, 
														acLineEndWithSegment.getAclnsegId(), null, bvId);
											}else {
									
												int node1InlineCount = node1.getInLines().size();
												int node2InlineCount = node2.getInLines().size();
												System.out.println("两站都有进线，找进线少的那个作为首站 node1InlineCount="+node1InlineCount
														+" node2InlineCount="+node2InlineCount);
												if (node1InlineCount<node2InlineCount) {
													istNode = node1;
													jstNode = node2;
												}else {
													istNode = node2;
													jstNode = node1;
												}
												//都有进线，随机找站作为首站测试
												//bothNodeHasInline(istNode, jstNode, false, acLineEndWithSegment);
												//debug move before
												boolean result = confirmIstNodeForBothHasInline(istNode, acLineEndWithSegment.getAclnsegId(), null, bvId);
												if (result) {
													setVirtualInOutLineByConfirmedIstNode(istNode, jstNode, 
															acLineEndWithSegment.getAclnsegId(), null, bvId);
												}else {
													setVirtualInOutLineByConfirmedIstNode(jstNode, istNode, 
															acLineEndWithSegment.getAclnsegId(), null, bvId);
												}
											}
										}
									}
								}
							}
                		}
                		
                	}
        			
        		}
        		
			}
    	}
    	
    }



    //往每个变电站中添加运行的主变
    public void addRunningPowerTransformerToSubstation(){
        int twoRunningWindingPowerCount = 0;
        int threeRunningWindingPowerCount = 0;
        for(TransformerSubstation transformerSubstation : transformerSubstationList){
            String stId = transformerSubstation.getStId();
            List<PowerTransformerWithWinding> runningTransformers = new ArrayList<>();
            for (PowerTransformerWithWinding powerTransformerWithWinding: powerTransformerWithWindingList){
                String stIdInPower = powerTransformerWithWinding.getStId();
                String powerName = powerTransformerWithWinding.getName();
                String powerType = powerTransformerWithWinding.getWindType();
                List<TransformerWinding> windings = powerTransformerWithWinding.getWindings();
                
                if (stId.equals(stIdInPower)){
                	//判断是否为用户变
                    boolean isUserSub = false;
                    for(String id: DMQuery.userSubstationId) {
                    	if (stId.equals(id)) {
                    		isUserSub = true;
                    		break;
						}
                    }
                    
                    if (isUserSub) {
                    	//对于用户变，只需要判断高压侧是否闭合，即可以确定是否运行
                        for (TransformerWinding transformerWinding : windings){
                            String YXStatus = transformerWinding.getYx_close();
                            String windType = transformerWinding.getWindType();
                            if (Constants.WINDING_HIGH.equals(windType) &&
                            		Constants.WINDING_YX_CLOSE.equals(YXStatus)) {
                        		System.out.println("添加运行时主变是用户变powerName="+powerName);
                            	runningTransformers.add(powerTransformerWithWinding);
							}
                        }
					}else {
		                    if (Constants.POWER_TYPE_TWO.equals(powerType)){
//		                        int windsCount = windings.size();
//		                        int index = 0;
//		                        for (TransformerWinding transformerWinding : windings){
//		                            String YXStatus = transformerWinding.getYx_close();
//		                            //如果有没有闭合的，则主变没有在运行，直接跳出
//		                            if (!Constants.WINDING_YX_CLOSE.equals(YXStatus)){
//		                                break;
//		                            }
//		                            index++;
//		                        }
//		                        //两圈变，都闭合的，则在运行
//		                        if (index == windsCount){
//		                            System.out.println("添加一个在运行的两圈变 windingName="+windingName);
//		                            runningTransformers.add(powerTransformerWithWinding);
//		                            twoRunningWindingPowerCount++;
//		                        }
		                        // TODO: 修改两圈变规则，只要低压侧闭合则认为运行
		                    	boolean needSetHighWindingClose = false;
		                        for (TransformerWinding transformerWinding : windings){
		                            String YXStatus = transformerWinding.getYx_close();
		                            String windType = transformerWinding.getWindType();
		                            if (Constants.WINDING_LOW.equals(windType) &&
		                            		Constants.WINDING_YX_CLOSE.equals(YXStatus)) {
		                            	runningTransformers.add(powerTransformerWithWinding);
		                            	// TODO: 将高压侧设置为闭合
		                            	needSetHighWindingClose = true;
									}
		                        }
		                        
		                        if (needSetHighWindingClose) {
		                            for (TransformerWinding transformerWinding : windings){
		                                String windType = transformerWinding.getWindType();
		                                if (Constants.WINDING_HIGH.equals(windType)) {
		                                	//将高压侧设置为闭合
		                                	transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
		    							}
		                            }
								}
		                        
		                    }else if (Constants.POWER_TYPE_THREE.equals(powerType)){
		                        //三圈变，需要高压侧和中低压侧有一侧闭合
		                        boolean highClose = false;
		                        boolean middleClose = false;
		                        boolean lowClose = false;
		                        for (TransformerWinding transformerWinding : windings){
		                            String YXStatus = transformerWinding.getYx_close();
		                            String windType = transformerWinding.getWindType();

		                            if (Constants.WINDING_YX_CLOSE.equals(YXStatus)&&
		                                    Constants.WINDING_HIGH.equals(windType)){
		                                highClose = true;
		                            }else if(Constants.WINDING_YX_CLOSE.equals(YXStatus)&&
		                                    Constants.WINDING_MIDDLE.equals(windType)){
		                                middleClose = true;
		                            }else if(Constants.WINDING_YX_CLOSE.equals(YXStatus)&&
		                                    Constants.WINDING_LOW.equals(windType)){
		                                lowClose = true;
		                            }
		                        }

		                        if ((highClose&&middleClose) || (highClose&&lowClose)){
		                            System.out.println("添加一个在运行的三圈变 powerName="+powerName);
		                            runningTransformers.add(powerTransformerWithWinding);
		                            threeRunningWindingPowerCount++;
		                        }
		                    }else {
		                    	System.out.println("判断运行时主变时遇到非法windType数据 powerName="+powerName+" windType="+powerType);
		                    }
					}

                }
            }
            //将各个变电站运行的主变添加进去
            transformerSubstation.setRunningTransformers(runningTransformers);
        }

        System.out.println("变电站transformerSubstation个数："+transformerSubstationList.size()+
        " 总主变Power个数:"+ powerTransformerWithWindingList.size()+
        " 运行两圈变个数："+twoRunningWindingPowerCount+
        " 运行三圈变个数："+threeRunningWindingPowerCount);

    }




    /**
     * 将主变的各侧绕阻中的遥信状态查出来，判断主变是否运行
     * 1、根据主变编号关键字名称找断路器；
     * 2、根据绕阻ND找隔刀，找到隔刀，根据隔刀bayId找断路器，未果，再根据隔刀ind,jnd找断路器，未果，则使用隔刀查遥信；
     * 3、根据绕阻ND未找到隔刀，则根据ND找母线：有母线，则根据绕阻ND找断路器，无母线，则记录线变组，高压侧默认遥信闭合；
     * @param rs
     * @param stmt
     * @throws SQLException
     */
    public void putYXStatusToTransformerWinding(ResultSet rs, Statement stmt) throws SQLException{
        //构建带有绕阻信息的主变，然后查询各侧遥信，判断是否在运行，添加到对应站点
        for(PowerTransformer powerTransformer : powerTransformerList){
            String transformerId = powerTransformer.getId();
            // 通过主变名字确认编号 #1 #2 #3 #4
            String transformerName = powerTransformer.getName();
            List<TransformerWinding> transformerWindings = new ArrayList<>();
            for (TransformerWinding transformerWinding: transformerWindingList){
            	String windingName = transformerWinding.getName();
                String trId = transformerWinding.getTrId();
                String windingBvId = transformerWinding.getBvId();
                String windingNd = transformerWinding.getNd();
                String windingStId = transformerWinding.getStId();
                //用来判断是否为用户变，如果是的，只要处理高压侧
                String windingType = transformerWinding.getWindType();
                if (transformerId.equals(trId)){
                    // 将各侧的遥信状态顺带查询了，然后写入标志记录
                    //select id,name,bv_id from breaker where st_id=113997367262315238 and name like '%主变%'
                    //若根据stId 和名字中的关键字匹配找breaker,没有找到breaker,则需另行处理
                    //String stIdInWinding = transformerWinding.getStId();
                    if (null == windingStId){
                        continue;
                    }
                    //如果是用户变，并且非高压侧，不用处理
                    boolean isUserSub = false;
                    for(String id: DMQuery.userSubstationId) {
                    	if (windingStId.equals(id)) {
                    		isUserSub = true;
                    		System.out.println("用户变windingName="+windingName);
                    		break;
						}
                    }
                    if (!Constants.WINDING_HIGH.equals(windingType) && isUserSub) {
						continue;
					}
                    
                    // 提前判断是否为线变组,无母线则设置
                    List<Busbarsection> busbarsections = findBusbarsectionByBvIdAndstId(windingBvId, windingStId);
                    if (null == busbarsections || busbarsections.size()==0) {
                    	transformerWinding.setIsLineVariantGroup("1");
					}
                    
                    int breakerCount = breakerList.size();
                    //记录10KV是否找到过断路器
                    boolean foundBreakerEver10KV = false;
                    int index = 0;
                    //通过绕阻关键字匹配找断路器
                    for(Breaker breaker: breakerList){
                        //通过stId, bvId，name三者匹配
                        String breakerStId = breaker.getStId();
                        String breakerBvId = breaker.getBvId();
                        String breakerName = breaker.getName();
                        boolean nameMatch = false;
                        if (null != transformerName && null!= breakerName){
                            // 主变编号由于# 有区别，需要去除数字来判断
                            String transformerNum = null;
                            if (transformerName.contains(Constants.TRANSFORMER_POUND)){
                                int poundIndex = transformerName.indexOf(Constants.TRANSFORMER_POUND);
                                transformerNum = transformerName.substring(poundIndex+1,poundIndex+2);
                            }else if (transformerName.contains(Constants.TRANSFORMER_POUND_PRO)){
                                int poundIndex = transformerName.indexOf(Constants.TRANSFORMER_POUND_PRO);
                                transformerNum = transformerName.substring(poundIndex+1, poundIndex+2);
                            }else {
                                logger.error("变压器主变名字很奇特 transformerName="+transformerName);
                                break;
                            }
                            if (null != transformerNum){
                                nameMatch = (breakerName.contains(Constants.TRANSFORMER_POUND + transformerNum+ "主变")||
                                        breakerName.contains(Constants.TRANSFORMER_POUND_PRO + transformerNum+ "主变")) &&
                                		//长沙.环保变/110kV.#3主变顺控程序转冷备用, 增加断路器关键字，避免类似环保变这种找到其他断路器。
                                		breakerName.contains("断路器");
                            }

                        } else {
                            logger.error("变压器主变名字或者断路器名字为空！！！！！！");
                        }

                        if (windingStId.equals(breakerStId) &&
                        		windingBvId.equals(breakerBvId)&&
                                nameMatch){
                            System.out.println("绕阻 "+windingName+"查找到breaker name="+breakerName);
                            //找到断路器，查遥信
                            String breakerId = breaker.getId();
                            boolean result  = YXStatusEqualMoment(queryMoment,
                                    breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
                            
                            if (!windingBvId.equals(Constants.BV_ID_10)) {
                                if (result){
                                    transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
                                }else {
                                    transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
                                }
                                //将断路器bayId记录到绕阻中
                                transformerWinding.setBayIdForBus(breaker.getBayId());
                                break;
							}else {
								//对于10KV 需要将所有的断路器都找一遍，只要有闭合的，就将该侧绕阻设置为闭合
                                if (result){
                                	//找到一个闭合的就行了
                                    transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
                                    //将断路器bayId记录到绕阻中
                                    transformerWinding.setBayIdForBus(breaker.getBayId());
                                    break;
                                }else {
                                	//继续找其他断路器，循环计算增加
                                	index++;
                                	foundBreakerEver10KV = true;
                                	//先根据遥信记录为开，后面反正还会继续找，找到后会覆盖此次的结果，然后跳出循环。
                                    transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
                                    //将断路器bayId记录到绕阻中
                                    transformerWinding.setBayIdForBus(breaker.getBayId());
                                    continue;
                                } 
							}
                        }
                        index++;
                    }

                    //如果根据名称关键字在breaker表中没有找到,则需要找隔刀
                    if(breakerCount == index){  	
                     	// 10kv 的找到过断路器了，只是遥信全部为开的，也就断了念头不要再找了，直接加入主变记录
                    	if (foundBreakerEver10KV && windingBvId.equals(Constants.BV_ID_10)) {
                    		System.out.println("10KV的已经找到过断路器，只是全部为断开，记录到主变吧 windingName="+windingName);
						}else {
	                        if (windingBvId.equals(Constants.BV_ID_220)||
	                        		windingBvId.equals(Constants.BV_ID_110)||
	                                windingBvId.equals(Constants.BV_ID_35)||
	                                windingBvId.equals(Constants.BV_ID_10)){
	       
	                            System.out.println("绕阻 "+windingName+" 根据名称关键字没有找到breaker 需要找隔刀 windingNd="+windingNd
	                                    +" windingStId="+windingStId+" windingBvId="+windingBvId);

	                            // 根据绕阻ND找隔刀
	                            if ((null != windingNd) && !"-1".equals(windingNd)){
	                            	Disconnector disconnector = findDisconnectorByNdAndStIdBvId(windingNd, windingStId, windingBvId);
	                            	if (null != disconnector) {
	                            		judgeWindingStatusByDisconnector(disconnector, transformerWinding);
									}else {
										//根据ND找断路器
										System.out.println("绕阻 "+windingName+"通过ND没有找到隔刀，开始找断路器");
										Breaker breaker = findBreakerByNd(windingNd);
				                        if (null != breaker) {
				                        	String breakerBayId = breaker.getBayId();
				                        	System.out.println("线变组通过ND 找到断路器 breakerBayId="+breakerBayId);
				                        	transformerWinding.setBayIdForBus(breakerBayId);
										}else {
											System.out.println("线变组通过ND没有找到断路器");
										}
				                        //记录找到的bayId
	                                    Busbarsection busbarsection = findBusbarsectionByNd(windingNd);
	                                    //有母线
	                                    if (null != busbarsection){
	                                        String breakerId = breaker.getId();
	                                        boolean result  = YXStatusEqualMoment(queryMoment,
	                                                breakerId, Constants.COLUMN_ID_BREAKER, 0, true);
	                                        if (result){
	                                            transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
	                                        }else {
	                                            transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
	                                        }
	                                    }else {
	                                        //无母线，为线变组
	                                        System.out.println("线变组 windingName="+ windingName+"  windingNd="+windingNd);
	                                    }
	                                
									}
	                            }else {
	                                // TODO: 绕阻ND=-1, 则判断绕阻的bvId， 如果为110，查隔刀关键字 "5X0X", X为主变编号
	                                //220-->6x0x  110  5X0X
	                                System.out.println("绕阻 "+windingName+" ND为空通过隔刀关键字如5X0X找隔刀，windingStId="+windingStId+" windingBvId="+windingBvId);
	                                String windingNumber = Constants.getPowerNumber(windingName);
	                                //隔刀末位编号规律改为#1 #2 5X0X, #3 5X04
	                                String lastNumber = windingNumber;
	                                if ("3".equals(windingNumber)) {
	                                	lastNumber = "4";
									}
	                                String disconnetorNameLike = null;
	                                if (null != windingNumber){
	                                    if (Constants.BV_ID_220.equals(windingBvId)){
	                                        disconnetorNameLike = "6"+windingNumber+"0"+lastNumber;
	                                    }else if(Constants.BV_ID_110.equals(windingBvId)){
	                                        disconnetorNameLike = "5"+windingNumber+"0"+lastNumber;
	                                    }else if(Constants.BV_ID_35.equals(windingBvId)){
	                                        disconnetorNameLike = "4"+windingNumber+"0"+lastNumber;
	                                    }else if(Constants.BV_ID_10.equals(windingBvId)){
	                                        disconnetorNameLike = "3"+windingNumber+"0"+lastNumber;
	                                    }

	                                    Disconnector disconnector = findDisconnetorByNumberAndBvIdStId(windingBvId, windingStId, disconnetorNameLike);
	                                    if(null != disconnector){
	                                        judgeWindingStatusByDisconnector(disconnector, transformerWinding);
	                                    }else {
	                                        System.out.println("判断主变是否运行中通过隔刀编号关键字还是找不到windingName："+windingName);
	                                        // TODO: DEBUG 为了查找缺失数据，此处先默认运行
	                                        transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
	                                    }
	                                }
	                            }
	                        }else{
	                            System.out.println("绕阻bvId不需要关注的数据, 默认断开 nd="+transformerWinding.getNd()
	                                    +" stId="+transformerWinding.getStId()+" bvId="+transformerWinding.getBvId());
	                        }
						}
                    }
                    //经过遥信判断之后，绕阻对象的开合状态已经赋值
                    System.out.println("最终该侧绕阻 "+windingName+" bayIdForBus="+transformerWinding.getBayIdForBus());
                    transformerWindings.add(transformerWinding);
                }
            }

            PowerTransformerWithWinding powerTransformerWithWinding = new PowerTransformerWithWinding();
            powerTransformerWithWinding.setId(powerTransformer.getId());
            powerTransformerWithWinding.setName(powerTransformer.getName());
            powerTransformerWithWinding.setBvId(powerTransformer.getBvId());
            powerTransformerWithWinding.setBayId(powerTransformer.getBayId());
            powerTransformerWithWinding.setStId(powerTransformer.getStId());
            powerTransformerWithWinding.setWindType(powerTransformer.getWindType());
            powerTransformerWithWinding.setWindings(transformerWindings);
            powerTransformerWithWindingList.add(powerTransformerWithWinding);
        }
    }


    /**
     * 通过隔刀判断主变是否运行
     * @param disconnector 隔刀
     * @param transformerWinding 绕阻
     * @throws SQLException
     */
    private void judgeWindingStatusByDisconnector(Disconnector disconnector,
                                                  TransformerWinding transformerWinding)throws SQLException{
        //找到隔刀之后，还要先判断是否有断路器，如果有，则还需要判断断路器的遥信
        //只有再无断路器的情况下，才需要判断刀闸遥信
        String bayId = disconnector.getBayId();
        String disconnectorId = disconnector.getId();
        String ind = disconnector.getInd();
        String jnd = disconnector.getJnd();
        String windingName = transformerWinding.getName();
        System.out.println("绕阻 "+windingName+" 隔刀找到 id="+disconnectorId+" bayId="+bayId +" stId="+disconnector.getStId());

        //1、通过隔刀的bayId查找断路器，找到则使用断路器遥信
        Breaker breaker = findBreakerByBayId(bayId);
        if (null != breaker){
        	System.out.println("绕阻 "+windingName+" 通过隔刀bayId找到断路器");
            String breakerId = breaker.getId();
            boolean result  = YXStatusEqualMoment(queryMoment,
                    breakerId, Constants.COLUMN_ID_BREAKER, 0 ,true);
            if (result){
                transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
            }else {
                transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
            }
            //将断路器bayId记录到绕阻中
            transformerWinding.setBayIdForBus(breaker.getBayId());
        }else {
            //2、没找到，则继续用隔刀的ind或者jnd找断路器
            Breaker breakerByNd = findBreakerByIndAndJnd(ind, jnd);
            if (null != breakerByNd){
            	System.out.println("绕阻 "+windingName+" 通过隔刀ND找到断路器");
                String breakerId = breakerByNd.getId();
                boolean result  = YXStatusEqualMoment(queryMoment,
                        breakerId, Constants.COLUMN_ID_BREAKER, 0 ,true);
                if (result){
                    transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
                }else {
                    transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
                }
                //将断路器bayId记录到绕阻中
                transformerWinding.setBayIdForBus(breakerByNd.getBayId());
            }else {
                //3、如果没找到，直接用隔刀的遥信
            	System.out.println("绕阻 "+windingName+" 直接使用隔刀的遥信来判断");
                boolean result  = YXStatusEqualMoment(queryMoment,
                        disconnectorId, Constants.COLUMN_ID_DISCONNECTOR, 0 ,true);
                if (result){
                    transformerWinding.setYx_close(Constants.WINDING_YX_CLOSE);
                }else {
                    transformerWinding.setYx_close(Constants.WINDING_YX_OPEN);
                }
                //将隔刀bayId记录到绕阻中
                transformerWinding.setBayIdForBus(disconnector.getBayId());
            }
        }
    }



    public boolean YXStatusBeforeMoment(String date,
                                        String tableId, long columnId,  int YXCount) throws  SQLException{

        //根据tableId 和 columnId 计算遥信Id
        long tableIdNumeric = Long.valueOf(tableId);
        long yxId = tableIdNumeric | (columnId<<32);
//        System.out.println("YXStatusBeforeMoment tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId +" yxId:"+yxId);

        //小于时刻
        String sql = "SELECT  OCCUR_TIME, STATUS, MILLI_SECOND FROM ALARM.YX_BW WHERE YX_ID =  "+yxId
                + " and OCCUR_TIME<'"+ date + "'order by OCCUR_TIME DESC  limit 1 " ;

        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            Timestamp occurTime = rs.getTimestamp(1);
            long status = rs.getLong(2);
            String milliSecond = rs.getString(3);

            String occurTimeStr = occurTime.toString();
//            System.out.println("occurTime:"+occurTime+" milliSecond:"+milliSecond
//                    +" status:"+status +" yxId:"+yxId);
            //奇变偶不变
            if ((status == Constants.YX_CLOSE_2 ||
                    status == Constants.YX_CLOSE_4||
                    status == Constants.YX_CLOSE_8||
                    status == Constants.YX_CLOSE_10||
                    status == Constants.YX_CLOSE_14||
                    status == Constants.YX_CLOSE_16)&&
                    (YXCount%2==0)){
//                System.out.println("经过0次或者偶数次遥信变位判断为闭合，遥信状态status="+status+" YXCount="+YXCount);
                return  true;
            }else if((status == Constants.YX_OPEN_1 ||
                    status == Constants.YX_OPEN_3||
                    status == Constants.YX_OPEN_7||
                    status == Constants.YX_OPEN_9||
                    status == Constants.YX_OPEN_13||
                    status == Constants.YX_OPEN_15)&&
                    (YXCount%2!=0)){
//                System.out.println("经过奇数次遥信变位判断为闭合，status="+status);
                return true;
            }else if (status == Constants.YX_BIANWEI_24){
                //递归
//                System.out.println("###########开始递归查找遥信变位#############");
                //此处已经落到遥信变位一次
                YXCount++;
//                System.out.println("遥信状态status="+status+ " occurTime:"+occurTime+" milliSecond="+milliSecond
//                        +" YXCount="+YXCount);
                //此处又是去查询等于，相当于同一条时间数据被重复取了一次，因为它包含了此前的那条小于上一个不同时间的记录
                //所以在等于查询中加了递归调用的判断，当又有查询到等于的记录时，则先做减一处理，放置重复计数。
                boolean result = YXStatusEqualMoment(occurTimeStr,  tableId,
                        columnId, YXCount,false);
                return result;
            }else {
//                System.out.println("遥信状态为开或者非关注数据status="+status);
            }
        }
        return false;
    }

    //通过内容content字段来判断遥信状态
    List<String> skipYX = new ArrayList<>();
    public boolean YXStatusQueryByContent(String date,
                                        String tableId, long columnId) throws  SQLException {

    	//debug
//    	if (true) {
//			return true;
//		}
    	
        //根据tableId 和 columnId 计算遥信Id
        long tableIdNumeric = Long.valueOf(tableId);
        long yxId = tableIdNumeric | (columnId << 32);
        String yxIdStr = String.valueOf(yxId);
        System.out.println("YXStatusQueryByContent tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId +" yxId:"+yxIdStr);
        
        //TODO:某些遥信查不出来或者时间太久，先默认闭合，跳过
        //YXStatusQueryByContent tableIdNumeric:114560317215739410 columnId:40 yxId:114560489014431250
        //夏铎铺
        if ("114560489014431250".equals(yxIdStr)) {
        	System.out.println("夏铎铺YXStatusQueryByContent 跳过的遥信");
        	return false;
		}
        
        //仙姑岭
        if ("114560489014431250".equals(yxIdStr)) {
        	System.out.println("仙姑岭YXStatusQueryByContent 跳过的遥信");
        	return true;
		}
        
        //长沙.夏铎铺 长沙.夏铎铺/10kV.＃1主变310断路器
        if ("114560489014431974".equals(yxIdStr)) {
        	System.out.println("夏铎铺YXStatusQueryByContent 跳过的遥信");
        	return true;
		}
        
        //长沙.仙姑岭 长沙.仙姑岭/10kV.#1主变310断路器 
        if ("114560489014429779".equals(yxIdStr)) {
        	System.out.println("仙姑岭YXStatusQueryByContent 跳过的遥信");
        	return true;
		}
        
        

        //小于等于时刻
        String sql = "SELECT  OCCUR_TIME, CONTENT FROM ALARM.YX_BW WHERE YX_ID =  " + yxIdStr
                + " and OCCUR_TIME<='" + date + "'order by OCCUR_TIME DESC  limit 1 ";

        // 执行查询
        rs = stmt.executeQuery(sql);
        int index = 0;
        while (rs.next()) {
        	index++;
            Timestamp occurTime = rs.getTimestamp(1);
            String content = rs.getString(2);

            if (null != content) {
                if (content.contains("合闸")) {
                    return true;
                }

                if (content.contains("分闸")) {
                	
                	//调试数据，显示哪些查到的是断开的
                	if(columnId == Constants.COLUMN_ID_BREAKER) {
                		for(Breaker breaker: breakerList) {
                			String breakerId = breaker.getId();
                			String breakerName = breaker.getName();
                			if (tableId.equals(breakerId)) {
                	        	System.out.println("有遥信数据 查到为断开YXStatusQueryByContent tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId 
                	        			+" yxId:"+yxIdStr+" breakerName="+breakerName);
        					}
                		}
                	}else if (columnId == Constants.COLUMN_ID_DISCONNECTOR) {
        				for(Disconnector disconnector: disconnectorList) {
        					String disconnetorId = disconnector.getId();
        					String disconnetorName = disconnector.getName();
        					if (tableId.equals(disconnetorId)) {
                	        	System.out.println("有遥信数据 查到为断开YXStatusQueryByContent tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId 
                	        			+" yxId:"+yxIdStr+" disconnetorName="+disconnetorName);
        					}
        				}
        			}
                	
                    return false;
                }
            } else {
                return false;
            }
        }
        
        if (index == 0) {

        	if(columnId == Constants.COLUMN_ID_BREAKER) {
        		for(Breaker breaker: breakerList) {
        			String breakerId = breaker.getId();
        			String breakerName = breaker.getName();
        			if (tableId.equals(breakerId)) {
        	        	System.out.println("无遥信数据 YXStatusQueryByContent tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId 
        	        			+" yxId:"+yxIdStr+" breakerName="+breakerName);
					}
        		}
        	}else if (columnId == Constants.COLUMN_ID_DISCONNECTOR) {
				for(Disconnector disconnector: disconnectorList) {
					String disconnetorId = disconnector.getId();
					String disconnetorName = disconnector.getName();
					if (tableId.equals(disconnetorId)) {
        	        	System.out.println("无遥信数据 YXStatusQueryByContent tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId 
        	        			+" yxId:"+yxIdStr+" disconnetorName="+disconnetorName);
					}
				}
			}
		}
        
        //无遥信数据，默认为闭合
        return true;
    }


    /**
     * 计算遥信状态中由于会存在时间相等，但是毫秒字段数据不等的情况，因为我们的查询逻辑需要修改为
     * 从等于需要查询的时刻方式查起，查询到的结果无非三种情况： 0 ， 1 ， n 条记录。
     * 对于0条记录，则继续查询小于当前时刻的数据，只用取排在最前面的一条，对于查询到等于当前记录有
     * 1或者多条记录，则一次查询其记录中的status，如果是遥信变位，记录次数，并且一直递归到不为遥信变位状态为止。
     * 在遍历完查询到等于时刻的记录时，如果还需要递归，则又需要查询小于当前时刻的记录，如此则会产生重复记录遥信变位的情况
     * 因为在查询小于某个时刻的方法中又递归调用了等于查询，则等于查询到的第一条数据肯定和小于查询的那个时刻重复计数了，
     * 需要在等于查询中判断递归调用并且查询到有结果的情况下减一处理。
     * @param date
     * @param rs
     * @param stmt
     * @param tableId
     * @param columnId
     * @param YXCount  遥信变位次数
     * @param isFirstCall  是否首次调用，即非递归
     * @return
     * @throws SQLException
     */
    public boolean YXStatusEqualMoment(String date, String tableId, long columnId,
                                       int YXCount, boolean isFirstCall) throws  SQLException{
    	
        // TODO: 使用content逻辑
        if(true){
        	long start = System.currentTimeMillis();
            boolean result = YXStatusQueryByContent(date, tableId, columnId);
            long end = System.currentTimeMillis();
            System.out.println("YXStatusQueryByContent spent time :"+(end-start)+" 毫秒");
            return result;
        }

        //根据tableId 和 columnId 计算遥信Id
        long tableIdNumeric = Long.valueOf(tableId);
        long yxId = tableIdNumeric | (columnId<<32);
        System.out.println("YXStatusEqualMoment tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId +" yxId:"+yxId);

        //等于时刻
        String sql = "SELECT  OCCUR_TIME, STATUS, MILLI_SECOND FROM ALARM.YX_BW WHERE YX_ID =  "+yxId
                + " and OCCUR_TIME='"+ date + "'order by MILLI_SECOND DESC " ;

        // 执行查询
        rs = stmt.executeQuery(sql);
        //记录当前遍历的条数位置
        int index = 0;
        //移动到最后
        rs.last();
        int resultCount = rs.getRow();
//        System.out.println("YXStatusEqualMoment resultCount="+resultCount);
        //递归调用时去除查询到 一条或者多条 等于当前时刻的记录时，重复第一条增加遥信变位次数
        if(resultCount >=1 && !isFirstCall){
            YXCount--;
        }
        //移动回来到第一条
        rs.beforeFirst();
        while (rs.next()){
            index++;
            Timestamp occurTime = rs.getTimestamp(1);
            long status = rs.getLong(2);
            String milliSecond = rs.getString(3);

            String occurTimeStr = occurTime.toString();
//            System.out.println("occurTime:"+occurTime+" milliSecond:"+milliSecond
//                    +" status:"+status +" yxId:"+yxId);
            //奇变偶不变
            if ((status == Constants.YX_CLOSE_2 ||
                    status == Constants.YX_CLOSE_4||
                    status == Constants.YX_CLOSE_8||
                    status == Constants.YX_CLOSE_10||
                    status == Constants.YX_CLOSE_14||
                    status == Constants.YX_CLOSE_16)&&
                    (YXCount%2==0)){
//                System.out.println("经过0次或者偶数次遥信变位判断为闭合，遥信状态status="+status+" YXCount="+YXCount);
                return  true;
            }else if((status == Constants.YX_OPEN_1 ||
                    status == Constants.YX_OPEN_3||
                    status == Constants.YX_OPEN_7||
                    status == Constants.YX_OPEN_9||
                    status == Constants.YX_OPEN_13||
                    status == Constants.YX_OPEN_15)&&
                    (YXCount%2!=0)){
//                System.out.println("经过奇数次遥信变位判断为闭合，status="+status);
                return true;
            } else if (status == Constants.YX_BIANWEI_24){
                //需要继续取
//                System.out.println("###########开始递归查找遥信变位#############");
                //所有落入遥信变位status的状态都进行次数加一
                YXCount++;
                //如果相等的记录还没完，则继续查询相等的，如果完了，则要查询小于此时刻的
                if (index<resultCount){
//                    System.out.println("遥信状态status="+status+ " occurTime:"+occurTime+" milliSecond="+milliSecond
//                            +" YXCount="+YXCount);
                    continue;
                }else {
//                    System.out.println("遥信状态status="+status+ " occurTime:"+occurTime+" milliSecond="+milliSecond
//                            +" YXCount="+YXCount);
                    boolean result = YXStatusBeforeMoment(occurTimeStr, tableId, columnId, YXCount);
                    return  result;
                }
            }else {
//                System.out.println("遥信状态为开或者非关注数据status="+status);
            }
        }

        // 等于此时刻的没有记录，则查小于，limit 1
        if(index == 0){
            boolean result = YXStatusBeforeMoment(date, tableId, columnId, YXCount);
            return  result;
        }

        return false;
    }
    
    
    
    //遥测查询
    public YCData YCQuery(String transformerWindingId, long columnId) throws  SQLException {

    	YCData ycData = new YCData();
        //根据tableId 和 columnId 计算遥信Id
        long windingIdNumeric = Long.valueOf(transformerWindingId);
        long ycId = windingIdNumeric | (columnId << 32);
//        System.out.println("YXStatusBeforeMoment tableIdNumeric:"+tableIdNumeric+" columnId:"+columnId +" yxId:"+yxId);

        //小于等于时刻
        String sql = "SELECT  HISTORY_TABLE_NAME, HISTORY_COLUMN_NAME FROM svr_yc_sample_define WHERE YC_ID =  " + ycId;

        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String historyTableName = rs.getString(1);
            String historyColumnName = rs.getString(2);
            
            ycData.setHistoryTableName(historyTableName);
            ycData.setHistoryColumnName(historyColumnName);

        }
        return ycData;
    }
    
    
    //查询遥测值
    
    public String YCValueQuery(String transformerWindingId, long columnId, 
    		String date) throws  SQLException {
    	YCData ycData = YCQuery(transformerWindingId, columnId);
    	String historyTableName = ycData.getHistoryTableName();
    	String historyColumnName = ycData.getHistoryColumnName();
    	String historyValue = null;
    	
    	if (null != historyTableName && null != historyColumnName) {
            //小于等于时刻
            String sql = "SELECT  OCCUR_TIME, " + historyColumnName +" from HISDB." + historyTableName
            		+ " WHERE OCCUR_TIME<='" + date + "'order by OCCUR_TIME DESC  limit 1 ";
            
            System.out.println("YCValueQuery sql="+sql);

            // 执行查询
            long start = System.currentTimeMillis();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
            	historyValue = rs.getString(2);
            	long end = System.currentTimeMillis();
            	System.out.println("YCValueQuery historyValue="+historyValue+" 耗时 "+(end-start)+" 毫秒");
            }
		}
    	    	
        return historyValue;
    }

}
