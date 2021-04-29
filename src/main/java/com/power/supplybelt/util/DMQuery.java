package com.power.supplybelt.util;

import com.power.supplybelt.entity.*;
import com.power.supplybelt.web.TransformerSubstationConntroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DMQuery {
	
	//数据库原始数据，不做任何过滤
    public static List<Breaker> originalBreakerList = new ArrayList<>();
    public static List<ACLineEnd> originalAcLineEndList = new ArrayList<>();
    public static List<ACLineSegment> originalAcLineSegmentList = new ArrayList<>();
    public static List<Substation> originalSubstationList = new ArrayList<>();
    public static List<PowerTransformer> originalPowerTransformerList = new ArrayList<>();
    public static List<TransformerWinding> originalTransformerWindingList = new ArrayList<>();
    public static List<Disconnector> originalDisconnectorList = new ArrayList<>();
    public static List<Busbarsection> originalBusbarsectionList = new ArrayList<>();
    //记录用户变站ID
    public static List<String> userSubstationId = new ArrayList<>();
    
    //具有主和备的站，统一转换为一个站ID

    private static final String ST_ID_WEILIN = "113997367262314610";
    private static final String ST_ID_WEILIN_BEI = "113997367262315220";
    private static final String ST_ID_WEILIN_COMBINE = "威灵合并站ID";
    
    private static final String ST_ID_NANZHUTANG = "113997367262314604";
    private static final String ST_ID_NANZHUTANG_BEI = "113997367262314908";
    private static final String ST_ID_NANZHUTANG_COMBINE = "楠竹塘合并站ID";
    
    private static final String ST_ID_LEIFENG = "113997367262314529";
    private static final String ST_ID_LEIFENG_BEI = "113997367262314859";
    private static final String ST_ID_LEIFENG_COMBINE = "雷锋合并站ID";
    
    private static final String ST_ID_SHANGDALONG = "113997367262314537";
    private static final String ST_ID_SHANGDALONG_BEI = "113997367262314914";
    private static final String ST_ID_SHANGDALONG_COMBINE = "上大垅合并站ID";
    
    private static final String ST_ID_JINWANZI = "113997367262314513";
    private static final String ST_ID_JINWANZI_BEI = "113997367262314921";
    public static final String ST_ID_JINWANZI_COMBINE = "jinwanzi合并站ID";
    
    
    private static final String ST_ID_HUANGHUA = "113997367262314500";
    private static final String ST_ID_HUANGHUA_BEI = "113997367262315238";
    private static final String ST_ID_HUANGHUA_COMBINE = "黄花合并站ID";
    
    
    private static final String ST_ID_XINAN = "113997367262314542";
    private static final String ST_ID_XINAN_BEI = "113997367262315263";
    private static final String ST_ID_XINAN_COMBINE = "新安合并站ID";
    
    private static final String ST_ID_HUOYAN = "113997367262314544";
    private static final String ST_ID_HUOYAN_BEI = "113997367262314666";
    private static final String ST_ID_HUOYAN_COMBINE = "火焰合并站ID";
    
    
    private static String[] needConvertStIdStation = {
    		ST_ID_WEILIN, ST_ID_WEILIN_BEI,
    		ST_ID_NANZHUTANG, ST_ID_NANZHUTANG_BEI,
//    		ST_ID_LEIFENG, ST_ID_LEIFENG_BEI,
//    		ST_ID_SHANGDALONG, ST_ID_SHANGDALONG_BEI,
    		ST_ID_JINWANZI, ST_ID_JINWANZI_BEI,
    		ST_ID_HUANGHUA, ST_ID_HUANGHUA_BEI,
    		ST_ID_XINAN, ST_ID_XINAN_BEI,
    		ST_ID_HUOYAN, ST_ID_HUOYAN_BEI};
    
    
    //鹤鸣主变名称
    private static final String HEIMING_WINDING_3 ="长沙.鹤鸣变/110kV.#3主变-高";
    //自定义ND
    private static final String HEMING_ND_POWER1 = "鹤鸣#1主变高压侧ND";
    private static final String HEMING_ND_POWER2 = "鹤鸣#2主变高压侧ND";
    private static final String HEMING_ND_POWER3 = "鹤鸣#3主变高压侧ND";
    // 主变绕阻侧的隔刀
	//长沙.鹤鸣变/110kV.5101隔离开关
    private static final String hemingDisconnectorId1 = "114841792192451413";
	//长沙.鹤鸣变/110kV.5202隔离开关
    private static final String hemingDisconnectorId2 = "114841792192451414";
	//长沙.鹤鸣变/110kV.5304隔离开关
    private static final String hemingDisconnectorId3 = "114841792192451412";
    //end侧的隔刀
    //长沙.鹤鸣变/110kV.5021隔离开关
    private static final String hemingEndDisconnectorId1 = "114841792192451396";
    //长沙.鹤鸣变/110kV.5044隔离开关
    private static final String hemingEndDisconnectorId2 = "114841792192451410";
    //breaker 连接的隔刀
    // I II 母
    //长沙.鹤鸣变/110kV.5001隔离开关
    private static final String hemingBreakerDisconnectorId1 = "114841792192451401";
    //长沙.鹤鸣变/110kV.5002隔离开关
    private static final String hemingBreakerDisconnectorId2 = "114841792192451402";
    // II IV母
    //长沙.鹤鸣变/110kV.5402隔离开关
    private static final String hemingBreakerDisconnectorId3 = "114841792192451415";
    //长沙.鹤鸣变/110kV.5404隔离开关
    private static final String hemingBreakerDisconnectorId4 = "114841792192451416";

    
    //长沙.鹤鸣变/110kV.110kVⅠ母
    private static final String hemingbusbarsectionId1 = "115404742145868481";
    //长沙.鹤鸣变/110kV.110kVⅡ母
    private static final String hemingbusbarsectionId2 = "115404742145868482";
    //长沙.鹤鸣变/110kV.110kVⅣ母
    private static final String hemingbusbarsectionId3 = "115404742145868485";
    //长沙.鹤鸣变/110kV.鹤鸣变_榔鹤杨Ⅰ线502
    private static final String hemingEndId1 = "116812117029422488";
    //长沙.鹤鸣变/110kV.鹤鸣变_榔鹤杨Ⅱ线504
    private static final String hemingEndId2 = "116812117029422448";
    //breaker
    //长沙.鹤鸣变/110kV.母联500断路器
    private static final String hemingBreakerId1 = "114560317215740072";
    //长沙.鹤鸣变/110kV.母联540断路器
    private static final String hemingBreakerId2 = "114560317215740075";
    

	
    //由于主备站合并，实际用的是备站的设备，需要将主的断路器去除
    //去除新安主的断路器
    private static final	String xinanBreakerId1 = "114560317215737102";
    private static final	String xinanBreakerId2 = "114560317215737105";
    private static final	String xinanBreakerId3 = "114560317215737141";
    private static final	String xinanBreakerId4 = "114560317215737142";
    //去除井湾子主的断路器
    private static final	String jingwanziBreakerId1 = "114560317215740306";
    private static final	String jingwanziBreakerId2 = "114560317215740307";
    //去除黄花变主的断路器
    private static final	String huanghuaBreakerId1 = "114560317215737666";
    private static final	String huanghuaBreakerId2 = "114560317215737667";
    private static final	String huanghuaBreakerId3 = "114560317215737663";
    private static final	String huanghuaBreakerId4 = "114560317215737662";
    private static final	String huanghuaBreakerId5 = "114560317215737665";
    private static final	String huanghuaBreakerId6 = "114560317215737664";
    private static final	String huanghuaBreakerId7 = "114560317215745881";
    //去除黄花变主变只留下备
    private static final	String huanghuaPowerId1 = "117093592006131827";
    private static final	String huanghuaPowerId2 = "117093592006131828";
    private static final	String huanghuaPowerId3 = "117093592006132344";
    
    //去除楠竹塘主的断路器
    private static final	String nanzhutangBreakerId1 = "114560317215738161";
    private static final	String nanzhutangBreakerId2 = "114560317215738172";
    private static final	String nanzhutangBreakerId3 = "114560317215738189";
    private static final	String nanzhutangBreakerId4 = "114560317215738190";
    private static final	String nanzhutangBreakerId5 = "114560317215742960";
    private static final	String nanzhutangBreakerId6 = "114560317215742961";
    private static final	String nanzhutangBreakerId7 = "114560317215742962";
    private static final	String nanzhutangBreakerId8 = "114560317215738206";
    private static final	String nanzhutangBreakerId9 = "114560317215738207";
    private static final	String nanzhutangBreakerId10 = "114560317215738208";
    
    //去除楠竹塘变主变只留下备
    private static final	String nanzhutangPowerId1 = "117093592006131856";
    private static final	String nanzhutangPowerId2 = "117093592006132172";

	
    
    private static String[] needRemoveMainBreaker = {
    		xinanBreakerId1, xinanBreakerId2,
    		xinanBreakerId3, xinanBreakerId4,
    		jingwanziBreakerId1, jingwanziBreakerId2,
    		huanghuaBreakerId1, huanghuaBreakerId2,
    		huanghuaBreakerId3, huanghuaBreakerId4,
    		huanghuaBreakerId5, huanghuaBreakerId6,
    		huanghuaBreakerId7,
    		nanzhutangBreakerId1, nanzhutangBreakerId2,
    		nanzhutangBreakerId3, nanzhutangBreakerId4,
    		nanzhutangBreakerId5, nanzhutangBreakerId6,
    		nanzhutangBreakerId7, nanzhutangBreakerId8,
    		nanzhutangBreakerId9, nanzhutangBreakerId10,};
    
    
    private static String[] needRemovePower = {
    		huanghuaPowerId1, huanghuaPowerId2,
    		huanghuaPowerId3,
    		nanzhutangPowerId1, nanzhutangPowerId2};
    
    
    
	private static Logger logger = LoggerFactory.getLogger(DMQuery.class);
	
	
	//初始化查询所以原始静态数据
	public static void queryAllDeviceData(ResultSet rs, Statement stmt) throws SQLException {
		String sql = null;
        //查询线端表
        sql  = "select  id, name, bay_id, bv_id, aclnseg_id, nd, st_id from aclineend_test2";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            ACLineEnd acLineEnd = new ACLineEnd();
            String id = rs.getString(1);
            String name = rs.getString(2);
            String bayId = rs.getString(3);
            String bvId = rs.getString(4);
            String aclnsegId = rs.getString(5);
            String nd = rs.getString(6);
            String stId = rs.getString(7);


            acLineEnd.setId(id);
            acLineEnd.setName(name);
            acLineEnd.setBayId(bayId);
            acLineEnd.setBvId(bvId);
            acLineEnd.setAclnsegId(aclnsegId);
            acLineEnd.setNd(nd);
            acLineEnd.setStId(stId);
            originalAcLineEndList.add(acLineEnd);
        }
        System.out.println("originalAcLineEndList size="+originalAcLineEndList.size());
     
        //线段表
        sql  = "select  id, name, bv_id, ist_id, jst_id, ind, jnd, acline_id from aclinesegment";
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            ACLineSegment acLineSegment = new ACLineSegment();
            String id = rs.getString(1);
            String name = rs.getString(2);
            String bvId = rs.getString(3);
            String istId = rs.getString(4);
            String jstId = rs.getString(5);
            String ind = rs.getString(6);
            String jnd = rs.getString(7);
            String aclineId = rs.getString(8);

            acLineSegment.setId(id);
            acLineSegment.setName(name);
            acLineSegment.setBvId(bvId);
            acLineSegment.setIstId(istId);
            acLineSegment.setJstId(jstId);
            acLineSegment.setInd(ind);
            acLineSegment.setJnd(jnd);
            acLineSegment.setAclineId(aclineId);

            originalAcLineSegmentList.add(acLineSegment);
        }
        System.out.println("originalAcLineSegmentList size="+originalAcLineSegmentList.size());
        
        //查询厂站表
        sql  = "select  id, name, bv_id, st_type from substation ";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            Substation substation = new Substation();
            String id = rs.getString(1);
            String name = rs.getString(2);
            String bvId = rs.getString(3);
            String stType = rs.getString(4);
            //记录用户变站ID
            if (Constants.SUBSTATION_TYPE_USER.equals(stType)) {
				userSubstationId.add(id);
			}
            
            substation.setId(id);
            substation.setName(name);
            substation.setBvId(bvId);
            substation.setStType(stType);
            originalSubstationList.add(substation);
            
        }
        System.out.println("originalSubstationList size="+originalSubstationList.size());
        
        //查询断路器breaker
        sql  = "select  id, name, bay_id, bv_id, st_id, ind,jnd from breaker";
        // 执行查询
        rs = stmt.executeQuery(sql);
        List<Breaker> breakers = new ArrayList<>();
        while (rs.next()) {
            Breaker breaker = new Breaker();
            String id = rs.getString(1);
            String name = rs.getString(2);
            String bayId = rs.getString(3);
            String bvId = rs.getString(4);
            String stId = rs.getString(5);
            String ind = rs.getString(6);
            String jnd = rs.getString(7);

            breaker.setId(id);
            breaker.setName(name);
            breaker.setBayId(bayId);
            breaker.setBvId(bvId);
            breaker.setStId(stId);
            breaker.setInd(ind);
            breaker.setJnd(jnd);

            originalBreakerList.add(breaker);
        }
        System.out.println("originalBreakerList size="+originalBreakerList.size());
        
        
        //查询主变表
        sql  = "select  id, name, st_id, bv_id, bay_id, wind_type from powertransformer";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            String id = rs.getString(1);
            String name = rs.getString(2);
            String stId = rs.getString(3);
            String bvId = rs.getString(4);
            String bayId = rs.getString(5);
            String windType = rs.getString(6);
            
            
            PowerTransformer powerTransformer = new PowerTransformer();
            powerTransformer.setId(id);
            powerTransformer.setName(name);
            powerTransformer.setBvId(bvId);
            powerTransformer.setStId(stId);
            powerTransformer.setBayId(bayId);
            powerTransformer.setWindType(windType);
            originalPowerTransformerList.add(powerTransformer);
        }
        System.out.println("originalPowerTransformerList size="+originalPowerTransformerList.size());
        
        //查询绕阻
        sql  = "select  id, name, st_id, bv_id, bay_id, tr_id, mvanom, nd, wind_type from transformerwinding";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            String id = rs.getString(1);
            String name = rs.getString(2);
            String stId = rs.getString(3);
            String bvId = rs.getString(4);
            String bayId = rs.getString(5);
            String trId = rs.getString(6);
            String mvanom = rs.getString(7);
            String nd = rs.getString(8);
            String windType = rs.getString(9);

            TransformerWinding transformerWinding = new TransformerWinding();
            transformerWinding.setId(id);
            transformerWinding.setName(name);
            transformerWinding.setBvId(bvId);
            transformerWinding.setStId(stId);
            transformerWinding.setBayId(bayId);
            transformerWinding.setTrId(trId);
            transformerWinding.setMvanom(mvanom);
            transformerWinding.setNd(nd);
            transformerWinding.setWindType(windType);
            originalTransformerWindingList.add(transformerWinding);
        }
        System.out.println("originalTransformerWindingList size="+originalTransformerWindingList.size());
        
        //查询
        sql  = "select  id, name, st_id, bv_id, bay_id, ind, jnd from disconnector";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            String id = rs.getString(1);
            String name = rs.getString(2);
            String stId = rs.getString(3);
            String bvId = rs.getString(4);
            String bayId = rs.getString(5);
            String ind = rs.getString(6);
            String jnd = rs.getString(7);

            Disconnector disconnector = new Disconnector();
            disconnector.setId(id);
            disconnector.setName(name);
            disconnector.setBvId(bvId);
            disconnector.setStId(stId);
            disconnector.setBayId(bayId);
            disconnector.setInd(ind);
            disconnector.setJnd(jnd);
            originalDisconnectorList.add(disconnector);
        }
        
        System.out.println("originalDisconnectorList size="+originalDisconnectorList.size());
        
        //查询
        sql  = "select  id, name, st_id, bv_id, bay_id, nd from busbarsection";
        // 执行查询
        rs = stmt.executeQuery(sql);
        while (rs.next()){
            String id = rs.getString(1);
            String name = rs.getString(2);
            String stId = rs.getString(3);
            String bvId = rs.getString(4);
            String bayId = rs.getString(5);
            String nd = rs.getString(6);

            Busbarsection busbarsection = new Busbarsection();
            busbarsection.setId(id);
            busbarsection.setName(name);
            busbarsection.setBvId(bvId);
            busbarsection.setStId(stId);
            busbarsection.setBayId(bayId);
            busbarsection.setNd(nd);
            originalBusbarsectionList.add(busbarsection);
        }
        System.out.println("originalBusbarsectionList size="+originalBusbarsectionList.size());
        
        modifyOriginalData();

	}
	
	//返回转换后的站ID
	private static String getCombineStationId(String stId) {
		if (null == stId) {
			return null;
		}
		
		if (ST_ID_WEILIN.equals(stId)||
				ST_ID_WEILIN_BEI.equals(stId)) {
			return ST_ID_WEILIN_COMBINE;
		}
		
		if (ST_ID_NANZHUTANG.equals(stId)||
				ST_ID_NANZHUTANG_BEI.equals(stId)) {
			return ST_ID_NANZHUTANG_COMBINE;
		}
		
		
		if (ST_ID_SHANGDALONG.equals(stId)||
				ST_ID_SHANGDALONG_BEI.equals(stId)) {
			return ST_ID_SHANGDALONG_COMBINE;
		}
		
		
		if (ST_ID_JINWANZI.equals(stId)||
				ST_ID_JINWANZI_BEI.equals(stId)) {
			return ST_ID_JINWANZI_COMBINE;
		}
		
		
		if (ST_ID_HUANGHUA.equals(stId)||
				ST_ID_HUANGHUA_BEI.equals(stId)) {
			return ST_ID_HUANGHUA_COMBINE;
		}
		
		
		if (ST_ID_XINAN.equals(stId)||
				ST_ID_XINAN_BEI.equals(stId)) {
			return ST_ID_XINAN_COMBINE;
		}
		
		if (ST_ID_HUOYAN.equals(stId)||
				ST_ID_HUOYAN_BEI.equals(stId)) {
			return ST_ID_HUOYAN_COMBINE;
		}
		
		return stId;
		
	}
	
	//判断是否需要转换站ID
	private static boolean needConvertStId(String stId) {
		if (null != stId) {
			for(int i=0; i<needConvertStIdStation.length; i++) {
				if (stId.equals(needConvertStIdStation[i])) {
					System.out.println("needConvertStId stId="+stId);
					return true;
				}
			}
		}
		
		return false;
	}
	
	//修改原始数据，将威灵，楠竹塘，带有备的和其本身合并为一个站
	public static void modifyOriginalData() {
		
		for(Breaker breaker: originalBreakerList) {
			String stId = breaker.getStId();

			if (needConvertStId(stId)) {
				breaker.setStId(getCombineStationId(stId));
			}
		}
		
		for(ACLineEnd acLineEnd: originalAcLineEndList) {
			String stId = acLineEnd.getStId();

			
			if (needConvertStId(stId)) {
				acLineEnd.setStId(getCombineStationId(stId));
			}
		}
		
		for(ACLineSegment acLineSegment: originalAcLineSegmentList) {
			String istId = acLineSegment.getIstId();
			String jstId = acLineSegment.getJstId();
			
			
			if (needConvertStId(istId)) {
				acLineSegment.setIstId(getCombineStationId(istId));
			}
			
			if (needConvertStId(jstId)) {
				acLineSegment.setJstId(getCombineStationId(jstId));
			}
			
		}
		
		for(Substation substation: originalSubstationList) {
			String stId = substation.getId();
			
			if (needConvertStId(stId)) {
				substation.setId(getCombineStationId(stId));
			}
		}
		
		for(PowerTransformer powerTransformer: originalPowerTransformerList) {
			String stId = powerTransformer.getStId();
			
			if (needConvertStId(stId)) {
				powerTransformer.setStId(getCombineStationId(stId));
			}
			
			//中和变
			String zhongheStId = "113997367262314919";
			if (zhongheStId.equals(stId)) {
				powerTransformer.setWindType(Constants.POWER_TYPE_TWO);
			}
		}
		
		for(TransformerWinding transformerWinding: originalTransformerWindingList) {
			String stId = transformerWinding.getStId();

			
			if (needConvertStId(stId)) {
				transformerWinding.setStId(getCombineStationId(stId));
			}
		}
		
		for(Disconnector disconnector: originalDisconnectorList) {
			String stId = disconnector.getStId();

			if (needConvertStId(stId)) {
				disconnector.setStId(getCombineStationId(stId));
			}
		}
		
		for(Busbarsection busbarsection: originalBusbarsectionList) {
			String stId = busbarsection.getStId();

			
			if (needConvertStId(stId)) {
				busbarsection.setStId(getCombineStationId(stId));
			}
		}
		
		modifyAclineEndBayId();
	}
	
	
	//修改aclineend中的bayId数据
	public static void modifyAclineEndBayId() {
		String huanghuaEndId1 = "116812117029421549";
		//修改后的bayId
		String huanghuaEndBayId1 = "114278842239042632";
		String huanghuaEndId2 = "116812117029422485";
		String huanghuaEndBayId2 = "114278842239042944";
		
		String jingwanziEndId1 = "116812117029423373";
		String jingwanziEndBayId1 = "114278842239041718";
		String jingwanziEndId2 = "116812117029423377";
		String jingwanziEndBayId2 = "114278842239041719";
		
		String shangdalongEndId1 = "116812117029421301";
		String shangdalongEndBayId1 = "114278842239025779";
		String shangdalongEndId2 = "116812117029421303";
		String shangdalongEndBayId2 = "114278842239025780";
		
		String houzishiEndId1 = "116812117029421343";
		String houzishiEndBayId1 = "114278842239041424";
		String houzishiEndId2 = "116812117029423011";
		String houzishiEndBayId2 = "114278842239027173";
		
		String duiziEndId1 ="116812117029422910";
		String duiziEndBayId1 ="114278842239042916";
		
		String panfuEndId1 = "116812117029421574";
		String panfuBayId1 = "114278842239042054";
		
		String yazhouhuEndId1 = "116812117029421771";
		String yazhouhuBayId1 = "114278842239041160";
		
		for(ACLineEnd acLineEnd: originalAcLineEndList) {
			String endId = acLineEnd.getId();
			String bayId = acLineEnd.getBayId();
			String nd = acLineEnd.getNd();
			if (huanghuaEndId1.equals(endId)) {
				acLineEnd.setBayId(huanghuaEndBayId1);
				System.out.println("黄花变修改End中的BayId 从"+bayId+" 改为："+huanghuaEndBayId1);
			}
			
			if (huanghuaEndId2.equals(endId)) {
				acLineEnd.setBayId(huanghuaEndBayId2);
				System.out.println("黄花变修改End中的BayId 从"+bayId+" 改为："+huanghuaEndBayId2);
			}
			
			if (jingwanziEndId1.equals(endId)) {
				acLineEnd.setBayId(jingwanziEndBayId1);
				System.out.println("井湾子修改End中的BayId 从"+bayId+" 改为："+jingwanziEndBayId1);
			}
			
			if (jingwanziEndId2.equals(endId)) {
				acLineEnd.setBayId(jingwanziEndBayId2);
				System.out.println("井湾子修改End中的BayId 从"+bayId+" 改为："+jingwanziEndBayId2);
			}
			
			if (shangdalongEndId1.equals(endId)) {
				acLineEnd.setBayId(shangdalongEndBayId1);
				System.out.println("上大垅修改End中的BayId 从"+bayId+" 改为："+shangdalongEndBayId1);
			}
			
			if (shangdalongEndId2.equals(endId)) {
				acLineEnd.setBayId(shangdalongEndBayId2);
				System.out.println("上大垅修改End中的BayId 从"+bayId+" 改为："+shangdalongEndBayId2);
			}
			
			if (houzishiEndId1.equals(endId)) {
				acLineEnd.setBayId(houzishiEndBayId1);
				System.out.println("猴子石修改End中的BayId 从"+bayId+" 改为："+houzishiEndBayId1);
			}
			
			if (houzishiEndId2.equals(endId)) {
				acLineEnd.setBayId(houzishiEndBayId2);
				System.out.println("猴子石修改End中的BayId 从"+bayId+" 改为："+houzishiEndBayId2);
			}
			
			if (duiziEndId1.equals(endId)) {
				acLineEnd.setBayId(duiziEndBayId1);
				System.out.println("堆资变修改End中的BayId 从"+bayId+" 改为："+duiziEndBayId1);
			}
			
			if (panfuEndId1.equals(endId)) {
				acLineEnd.setBayId(panfuBayId1);
				System.out.println("蟠福变修改End中的BayId 从"+bayId+" 改为："+panfuBayId1);
			}
			
			if (yazhouhuEndId1.equals(endId)) {
				acLineEnd.setBayId(yazhouhuBayId1);
				System.out.println("亚洲湖变修改End中的BayId 从"+bayId+" 改为："+yazhouhuBayId1);
			}
			
			
			//修改鹤鸣变的end中的ND
			if (hemingEndId1.equals(endId)) {
				acLineEnd.setNd(HEMING_ND_POWER1);
				System.out.println("修改鹤鸣End nd从"+ nd+" 到"+HEMING_ND_POWER1);
			}
			// II 线连接的是 #3主变
			if (hemingEndId2.equals(endId)) {
				acLineEnd.setNd(HEMING_ND_POWER3);
				System.out.println("修改鹤鸣End nd从"+ nd+" 到"+HEMING_ND_POWER3);
			}
		}
	}
	
	public static void processACLineEnd(ResultSet rs, Statement stmt) throws SQLException {
        
        for(ACLineEnd acLineEnd: originalAcLineEndList) {
        	
        	  String id = acLineEnd.getId();
              String name = acLineEnd.getName();
              String bayId = acLineEnd.getBayId();
              String bvId = acLineEnd.getBvId();
              String aclnsegId = acLineEnd.getAclnsegId();
              String nd = acLineEnd.getNd();
              String stId = acLineEnd.getStId();


              //去除虚拟站
              if(Constants.T_STATION_ID_1.equals(stId)||
                      Constants.T_STATION_ID_2.equals(stId)||
                      Constants.T_STATION_ID_3.equals(stId)||
                      Constants.T_STATION_ID_4.equals(stId)||
                      Constants.T_STATION_ID_5.equals(stId)){
                  continue;
              }

              //去除500 和 220KV
              if (Constants.BV_ID_500.equals(bvId) ||
                      Constants.BV_ID_220.equals(bvId)){
                  System.out.println("去除bvId="+bvId);
                  continue;
              }

              //去除bayId和nd 都为null
//              if ("null".equals(bayId) &&
//                      "-1".equals(nd)){
//                  System.out.println("去除bayId="+bayId +" nd="+nd);
//                  continue;
//              }
              // TODO: 此处会过滤 高家塘 等站，所以先去除，后面进出线找不到的也要默认有一条线?
//              if ((null == bayId) &&
//              "-1".equals(nd)){
//                  System.out.println("去除bayId="+bayId +" nd="+nd);
//                  continue;
//              }
              
              if (null == bayId){
                  logger.warn("acLineEnd中bayId为空 stId="+stId+" name="+name);
               }
              TransformerSubstationConntroller.acLineEndList.add(acLineEnd);
        }
        System.out.println("查询线端总记录个数acLineEndList="+TransformerSubstationConntroller.acLineEndList.size());
        System.out.println("#####查询到的End数据集合#####");
        for (ACLineEnd ae: TransformerSubstationConntroller.acLineEndList){
            System.out.println("stId="+ ae.getStId()+" " + ae.getName() + " bayId=" + ae.getBayId() +" segId="+ae.getAclnsegId());
        }
	}

	
	 //查询线段
    public static void processACLineSegment(ResultSet rs, Statement stmt) throws SQLException{

        
        for(ACLineSegment acLineSegment: originalAcLineSegmentList) {
        	String segName = acLineSegment.getName();
        	//移除废线
        	if (segName.contains("(废)")) {
				continue;
			}
        	TransformerSubstationConntroller.acLineSegmentList.add(acLineSegment);
        }


        System.out.println("线段acLineSegment个数："+TransformerSubstationConntroller.acLineSegmentList.size());
        System.out.println("#####查询到的Segment数据集合#####");
        for(ACLineSegment acLineSegment: TransformerSubstationConntroller.acLineSegmentList){
            System.out.println("id="+acLineSegment.getId()+" name="+acLineSegment.getName());
        }

    }

    //查询厂站
    public static void processSubstation(ResultSet rs, Statement stmt) throws SQLException{
 
        
        // TODO:去除合并站之后重复的站,防止像存在威灵和威灵备这种名字不同，stid相同的两个站，导致供带后面找名字会随机找前面找到的。
    	List<String> hasAddSubstationId = new ArrayList<>();
        for(Substation substation: originalSubstationList) {
        	
            String id = substation.getId();
            String name = substation.getName();
            String bvId = substation.getBvId();
            if (Constants.BV_ID_220.equals(bvId)||
                    Constants.BV_ID_110.equals(bvId)||
                    Constants.BV_ID_35.equals(bvId)||
                    Constants.BV_ID_10.equals(bvId)){
                //在遥信过滤后的线端段表中的ST_ID才需要构建记录
                boolean stIdExistInSegment = false;
                for(ACLineEndWithSegment acLineEndWithSegment: TransformerSubstationConntroller.acLineEndWithSegmentList){
                    String stIdInSeg = acLineEndWithSegment.getStId();
                    if (id.equals(stIdInSeg)){
                        stIdExistInSegment = true;
                        break;
                    }
                }
                //如果厂站存在过滤后的端段结构中，则添加到集合
                if (stIdExistInSegment && !hasAddSubstationId.contains(id)){
                	hasAddSubstationId.add(id);
                    TransformerSubstationConntroller.substationList.add(substation);
                }
            }
        }

        System.out.println("220KV到10KV的厂站substationList个数："+TransformerSubstationConntroller.substationList.size());
        System.out.println("#####查询到的Substation数据集合#####");
        for(Substation substation: TransformerSubstationConntroller.substationList){
            System.out.println("id="+substation.getId()+" name="+substation.getName()+" bv_id="+substation.getBvId());
        }
    }

    //查询断路器
    public static void processBreaker(ResultSet rs, Statement stmt) throws SQLException{
 
        
        for(Breaker breaker: originalBreakerList) {

        	
        	String breakerId = breaker.getId();
        	String breakerName = breaker.getName();
        	String ind = breaker.getInd();
        	String jnd = breaker.getJnd();
        	boolean needRemove = false;
        	for(int i=0; i<needRemoveMainBreaker.length; i++) {
        		if (breakerId.equals(needRemoveMainBreaker[i])) {
        			needRemove = true;
        			break;
				}
        	}
        	if (needRemove) {
        		System.out.println("从原始数据中移除站主断路器："+breakerName);
				continue;
			}
        	
        	//修改鹤鸣母联断路器ind  jnd
        	if (hemingBreakerId1.equals(breakerId)) {
				breaker.setInd(HEMING_ND_POWER1);
				breaker.setJnd(HEMING_ND_POWER2);
				System.out.println("修改鹤鸣断路器"+breakerName+"ind从"+ind+" 到"+HEMING_ND_POWER1
						+" jnd从"+jnd+" 到"+HEMING_ND_POWER2);
			}
        	
        	if (hemingBreakerId2.equals(breakerId)) {
				breaker.setInd(HEMING_ND_POWER2);
				breaker.setJnd(HEMING_ND_POWER3);
				System.out.println("修改鹤鸣断路器"+breakerName+"ind从"+ind+" 到"+HEMING_ND_POWER2
						+" jnd从"+jnd+" 到"+HEMING_ND_POWER3);
			}
        	
        	TransformerSubstationConntroller.breakerList.add(breaker);
        }

        System.out.println("查询断路器总记录个数breakerList="+TransformerSubstationConntroller.breakerList.size());
        for (Breaker breaker: TransformerSubstationConntroller.breakerList){
            System.out.println("stId="+breaker.getStId()+" " + breaker.getName() + " bayId=" + breaker.getBayId());
        }
    }

     //查询主变
     public static void processPowerTransformer(ResultSet rs, Statement stmt) throws SQLException{

         
         for(PowerTransformer powerTransformer: originalPowerTransformerList) {
        	 
        	 String stId = powerTransformer.getStId();
        	 String bvId = powerTransformer.getBvId();
        	 String powerId = powerTransformer.getId();
        	 String powerName = powerTransformer.getName();
        	 boolean needRemove = false;
         	for(int i=0; i<needRemovePower.length; i++) {
        		if (powerId.equals(needRemovePower[i])) {
        			needRemove = true;
        			break;
				}
        	}
        	if (needRemove) {
        		System.out.println("从原始数据中移除主变："+powerName);
				continue;
			}
        	
        	
             //只要记录在过滤后的厂站中的主变
             boolean stIdExistInSubstation = false;
             for(Substation substation: TransformerSubstationConntroller.substationList){
                String substationId = substation.getId();
                if (substationId.equals(stId)){
                    stIdExistInSubstation = true;
                    break;
                }
             }

             boolean bvIdNeeded = false;
             if ((null != bvId) && (bvId.equals(Constants.BV_ID_220)||
                     bvId.equals(Constants.BV_ID_110)||
                     bvId.equals(Constants.BV_ID_35))){
                 bvIdNeeded = true;
             }
             
             
             if (stIdExistInSubstation && bvIdNeeded){
                 TransformerSubstationConntroller.powerTransformerList.add(powerTransformer);
             }
             
         }
         
         
         System.out.println("过滤后的主变powerTransformer个数："+TransformerSubstationConntroller.powerTransformerList.size());
         System.out.println("#####查询到的powerTransformer数据集合#####");
        for(PowerTransformer powerTransformer: TransformerSubstationConntroller.powerTransformerList){
            System.out.println("id="+powerTransformer.getId()+" name="+powerTransformer.getName()
                    +" stId="+powerTransformer.getStId() +" bv_id="+powerTransformer.getBvId());
        }

     }


    //查询绕阻
    public static void processTransformerwinding(ResultSet rs, Statement stmt) throws SQLException{
     
        for(TransformerWinding transformerWinding: originalTransformerWindingList) {
        	String trId = transformerWinding.getTrId();
        	String windingId = transformerWinding.getId();
            //只要记录在过滤后的主变中的绕阻
            boolean trIdExistInPower = false;
            for(PowerTransformer powerTransformer: TransformerSubstationConntroller.powerTransformerList){
                String transformerId = powerTransformer.getId();
                if (transformerId.equals(trId)){
                    trIdExistInPower = true;
                    break;
                }
            }
            
            //117375066982843836	NULL	长沙.鹤鸣变/110kV.3主变-高， 绕阻名称没有#
            if ("117375066982843836".equals(windingId)) {
            	transformerWinding.setName(HEIMING_WINDING_3);
			}
            
            
            if (trIdExistInPower){
                TransformerSubstationConntroller.transformerWindingList.add(transformerWinding);
            }
            
        }
        System.out.println("过滤后的绕阻transformerWinding个数："+TransformerSubstationConntroller.transformerWindingList.size());
        System.out.println("#####查询到的transformerWinding数据集合#####");
        for(TransformerWinding transformerWinding: TransformerSubstationConntroller.transformerWindingList){
            System.out.println("id="+transformerWinding.getId()+" name="+transformerWinding.getName()
                    +" stId="+transformerWinding.getStId() +" bv_id="+transformerWinding.getBvId());
        }

    }


    //查询隔刀
    public static void processDisconnector(ResultSet rs, Statement stmt) throws SQLException{
     
        for(Disconnector disconnector: originalDisconnectorList) {
        	String disconnectorId = disconnector.getId();
        	String disconnectorName = disconnector.getName();
        	String ind = disconnector.getInd();
        	//修改鹤鸣ND=-1的情况
        	if (hemingDisconnectorId1.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER1);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER1);
			}
        	
        	if (hemingDisconnectorId2.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER2);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER2);
			}
        	
        	if (hemingDisconnectorId3.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER3);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER3);
			}
        	
        	if (hemingEndDisconnectorId1.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER1);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER1);
			}
        	
        	if (hemingEndDisconnectorId2.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER3);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER3);
			}
        	
        	//母联断路器连接的隔刀
        	if (hemingBreakerDisconnectorId1.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER1);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER1);
			}
        	if (hemingBreakerDisconnectorId2.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER2);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER2);
			}
        	
        	if (hemingBreakerDisconnectorId3.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER2);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER2);
			}
        	if (hemingBreakerDisconnectorId4.equals(disconnectorId)) {
        		disconnector.setInd(HEMING_ND_POWER3);
        		System.out.println("修改鹤鸣隔刀"+disconnectorName+"ind从"+ind+" 到"+HEMING_ND_POWER3);
			}
        	
            TransformerSubstationConntroller.disconnectorList.add(disconnector);
        }
        System.out.println("过滤后的隔刀disconnector个数："+TransformerSubstationConntroller.disconnectorList.size());
//        for(Disconnector disconnector: disconnectorList){
//            System.out.println("id="+disconnector.getId()+" name="+disconnector.getName()
//                    +" stId="+disconnector.getStId() +" bv_id="+disconnector.getBvId());
//        }

    }


    //查询母线
    public static void processBusbarsection(ResultSet rs, Statement stmt) throws SQLException{
        
        for(Busbarsection busbarsection: originalBusbarsectionList) {
        	String busId = busbarsection.getId();
        	String busName = busbarsection.getName();
        	String nd = busbarsection.getNd();
        	
        	//修改鹤鸣ND=-1
        	if (hemingbusbarsectionId1.equals(busId)) {
				busbarsection.setNd(HEMING_ND_POWER1);
				System.out.println("修改鹤鸣母线"+busName+"nd从"+nd+" 到"+HEMING_ND_POWER1);
			}
        	
        	if (hemingbusbarsectionId2.equals(busId)) {
				busbarsection.setNd(HEMING_ND_POWER2);
				System.out.println("修改鹤鸣母线"+busName+"nd从"+nd+" 到"+HEMING_ND_POWER2);
			}
        	
        	if (hemingbusbarsectionId3.equals(busId)) {
				busbarsection.setNd(HEMING_ND_POWER3);
				System.out.println("修改鹤鸣母线"+busName+"nd从"+nd+" 到"+HEMING_ND_POWER3);
			}
            
            TransformerSubstationConntroller.busbarsectionList.add(busbarsection);
        }
        System.out.println("过滤后的母线busbarsection个数："+TransformerSubstationConntroller.busbarsectionList.size());
//        for(Busbarsection busbarsection: busbarsectionList){
//            System.out.println("id="+busbarsection.getId()+" name="+busbarsection.getName()
//                    +" stId="+busbarsection.getStId() +" bv_id="+busbarsection.getBvId());
//        }

    }
}
