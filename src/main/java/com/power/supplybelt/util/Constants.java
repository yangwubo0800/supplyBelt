package com.power.supplybelt.util;

import org.apache.commons.lang3.StringUtils;

public class Constants {

    //定义电压等级
    public static final String BV_ID_500 = "112871467355471873";
    public static final String BV_ID_220 = "112871467355471874";
    public static final String BV_ID_110 = "112871467355471875";
    public static final String BV_ID_35 = "112871467355471876";
    public static final String BV_ID_10 = "112871467355471877";

    //select * from sys_menu_info where menu_name='遥信变位登录状态'
    //合闸
    public static final long YX_CLOSE_2 = 2;
    public static final long YX_CLOSE_4 = 4;
    public static final long YX_CLOSE_8 = 8;
    public static final long YX_CLOSE_10 = 10;
    public static final long YX_CLOSE_14 = 14;
    public static final long YX_CLOSE_16 = 16;
    //分闸
    public static final long YX_OPEN_1 = 1;
    public static final long YX_OPEN_3 = 3;
    public static final long YX_OPEN_7 = 7;
    public static final long YX_OPEN_9 = 9;
    public static final long YX_OPEN_13 = 13;
    public static final long YX_OPEN_15 = 15;

    //遥信变位，奇数次则取反，偶数次不变
    public static final long YX_BIANWEI_24 = 24;

    //定义主变各侧绕阻的断路器或者隔刀的遥信状态
    public static final String WINDING_YX_OPEN = "0";
    public static final String WINDING_YX_CLOSE = "1";

    //定义绕阻三端状态
    public static final String WINDING_HIGH = "0";
    public static final String WINDING_MIDDLE= "1";
    public static final String WINDING_LOW = "2";

    //定义主变类型,两圈变和三圈变
    public static final String POWER_TYPE_TWO = "0";
    public static final String POWER_TYPE_THREE= "1";

    //主变编号
    public static final String TRANSFORMER_NUMBER_1= "#1";
    public static final String TRANSFORMER_NUMBER_2= "#2";
    public static final String TRANSFORMER_NUMBER_3= "#3";
    public static final String TRANSFORMER_NUMBER_4= "#4";
    public static final String TRANSFORMER_NUMBER_1_PRO= "＃1";
    public static final String TRANSFORMER_NUMBER_2_PRO= "＃2";
    public static final String TRANSFORMER_NUMBER_3_PRO= "＃3";
    public static final String TRANSFORMER_NUMBER_4_PRO= "＃4";
    public static final String TRANSFORMER_POUND= "#";
    public static final String TRANSFORMER_POUND_PRO= "＃";


    //遥信计算
    //    表ID	表名	中文名
    //    D5000模式下：
    //405	substation	厂站表
    //407	breaker	断路器表
    //408	disconnector	刀闸表
    //410	busbarsection	母线表
    //414	aclinesegment	交流线段表
    //415	aclineend	交流线段端点表
    //416	powertransformer	变压器表
    //417	transformerwinding	变压器绕组表
    public static final String TABLE_ID_BREAKER= "407";
    public static final String TABLE_ID_DISCONNECTOR= "408";
    //固定columnId SELECT COLUMN_ID FROM SYS_COLUMN_INFO WHERE TABLE_ID = 407 AND COLUMN_NAME_ENG = 'point'
    public static final long COLUMN_ID_BREAKER = 40;
    public static final long COLUMN_ID_DISCONNECTOR = 30;
    //SELECT COLUMN_ID FROM SYS_COLUMN_INFO WHERE TABLE_ID = 417 AND COLUMN_NAME_ENG = 'p'	
    public static final long COLUMN_ID_YC = 50;


    //虚拟站进行过滤
    public static final String T_STATION_ID_1 = "113997367262314588";
    public static final String T_STATION_ID_2 = "113997367262314589";
    public static final String T_STATION_ID_3 = "113997367262314590";
    public static final String T_STATION_ID_4 = "113997367262314591";
    public static final String T_STATION_ID_5 = "113997367262314554";
    
    //用户变值定义
    public static final String SUBSTATION_TYPE_USER = "8";

    public static String convertBvIdToVoltage(String bvId){
        if (StringUtils.isBlank(bvId)){
            return null;
        }

        if (BV_ID_220.equals(bvId)){
            return "220";
        }else if (BV_ID_110.equals(bvId)){
            return "110";
        }else if (BV_ID_35.equals(bvId)){
            return "35";
        }else if (BV_ID_10.equals(bvId)){
            return "10";
        }else {
            return "不关注";
        }
    }


    //通过主变或者绕阻名称，获取编号
    public static String getPowerNumber(String powerName){
        if (null == powerName){
            return null;
        }
        String powerNumber = null;
        if (powerName.contains(Constants.TRANSFORMER_POUND)){
            int poundIndex = powerName.indexOf(Constants.TRANSFORMER_POUND);
            powerNumber = powerName.substring(poundIndex+1,poundIndex+2);
        }else if (powerName.contains(Constants.TRANSFORMER_POUND_PRO)){
            int poundIndex = powerName.indexOf(Constants.TRANSFORMER_POUND_PRO);
            powerNumber = powerName.substring(poundIndex+1, poundIndex+2);
        }else {
            System.out.println("主变名称中没有胖瘦#");
            //长沙.鹤鸣变/110kV.3主变-高， 主变没有#
        }
        return powerNumber;
    }
    
    
    
    public static String[] lackStationName = {


    };

}
