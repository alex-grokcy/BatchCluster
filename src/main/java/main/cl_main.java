package main;

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

//import static org.apache.spark.sql.functions.date_format;

public class cl_main {
	
	//---------CONSTANTES---------//
	
	final static String gc_table 		= "LOG_UFSM";
	
	final static String gc_kmeans_ddos 	= "LOG_KMEANS_DDOS_UFSM";
	
	final static String gc_kmeans_scan 	= "LOG_KMEANS_SCAN_PORT_UFSM";
	
	final static String gc_totais 	    = "LOG_TOTAIS_UFSM";
	
	final static String gc_conn_ip 		= "CONN_IP1";
	
	final static String gc_stamp 		= "2019-01-01 13:00:00.000"; //por aqui ele considera o GMT -2 e no SQL no CMD é sem GMT
	
	final static String gc_http 		= "http";
	
	final static String gc_ssl 			= "ssl";
	
	final static String gc_ssh 			= "ssh";
	
	final static String gc_tcp 			= "tcp";
	
	final static String gc_udp 			= "udp";
	
	final static String gc_ts 			= "TS";
	
	//---------ATRIBUTOS---------//
	
	public long gv_stamp;
	
	private Date gv_time = new Date();
	
	public static int gv_submit = 0; //1=Cluster 
	
	private static int gv_batch = 3;
	
	private Dataset<Row> gt_data;
	
	private static SparkConf gv_conf;
    
	private static SparkContext gv_context;
	                        
	public static SparkSession gv_session;
	
	//---------ATRIBUTOS-CLASSES---------//
	
	private static cl_main gv_main;
	
	private cl_process go_processa;
	
	private cl_seleciona go_select;				
	
	//---------METODOS---------//
	
	public static void main(String[] args) throws AnalysisException {
		
		gv_main = new cl_main();
		
		gv_main.m_start();

	}
	
	public void m_start() throws AnalysisException {
		
		m_conf_spark();
		
		gv_stamp = gv_time.getTime();	//Stamp do inicio do processamento
		
		go_select = new cl_seleciona();
		
		go_processa = new cl_process(gc_stamp, gv_stamp);		
		
		cl_kmeans lo_kmeans;
		
		switch(gv_batch){

		case 1: //SALVA todos os dados em CSV CONN, DNS e HTTP
			
			go_select.m_conf_phoenix(gc_table, gv_session);
			
			gt_data = go_select.m_seleciona(gc_stamp);
	
			if(gt_data.count() > 0) {
			
				go_processa.m_get_all(gt_data);
	
			}
			
			break;
			
		case 3:
			
			Dataset<Row> lt_ipso;
			
			String lv_stampso = "2019-01-01 13:00:00.000";
			
			//String lc_format = "dd/MM/yyyy HH:mm";
						
			cl_util.m_time_start();
			
			go_select.m_conf_phoenix(gc_totais, gv_session);
			
			lt_ipso = go_select.m_select_LogTotais(lv_stampso);
			
			//###########################################
			//IpInfo
			//###########################################
			
			go_processa.m_search_orig_h(lt_ipso);// Com o IpInfo
			
			cl_util.m_time_end();
			
			break;
				
		case 4:
			
			Dataset<Row> lt_ips;
			
			String lv_stamps = "2019-01-01 13:00:00.000";
			
			//String lc_format = "dd/MM/yyyy HH:mm";
						
			cl_util.m_time_start();
			
			go_select.m_conf_phoenix(gc_totais, gv_session);
			
			lt_ips = go_select.m_select_LogTotais(lv_stamps);
			
			//###########################################
			//IpInfo
			//###########################################
			
			go_processa.m_export_resp_h(lt_ips);// Com o IpInfo
			
			cl_util.m_time_end();
			
			break;
				
		case 5: //Processa e salva as Análises na tabela
					
			go_select.m_conf_phoenix(gc_table, gv_session);
					
			gt_data = go_select.m_seleciona_conn(gc_stamp);
			
			go_processa.m_start_analyzes(gt_data);			
			
			break;
			
		case 6: //DDOS K-means								
			
			lo_kmeans = new cl_kmeans(gc_stamp, gv_stamp);
			
			go_select.m_conf_phoenix(gc_table, gv_session);
			
			gt_data = go_select.m_seleciona_conn(gc_stamp);
			
			lo_kmeans.m_start_kmeans_ddos(gv_session, gt_data, gc_http );
			
			lo_kmeans.m_start_kmeans_ddos(gv_session, gt_data, gc_ssh );
			
			//lo_kmeans.m_start_kmeans_ddos(gv_session, gt_data, gc_ssl );						
				
			break;
		
		case 7: //Portscan Kmeans
			
			lo_kmeans = new cl_kmeans(gc_stamp, gv_stamp);
			
			go_select.m_conf_phoenix(gc_table, gv_session);
			
			gt_data = go_select.m_seleciona_conn(gc_stamp);
			
			lo_kmeans.m_start_kmeans_ScanPort(gv_session, gt_data, gc_tcp );
			
			lo_kmeans.m_start_kmeans_ScanPort(gv_session, gt_data, gc_udp );
			
			break;
			
		case 8: //Get resultados
			
			Dataset<Row> lt_res;
						
			String lv_stamp = "2019-01-01 13:00:00.000";
			
			//String lc_format = "dd/MM/yyyy HH:mm";
						
			cl_util.m_time_start();
			
			go_select.m_conf_phoenix(gc_totais, gv_session);
			
			lt_res = go_select.m_select_LogTotais(lv_stamp);											
						
			//###########################################
			//IpInfo
			//###########################################
			
			go_processa.m_export_resp_h(lt_res);// Com o IpInfo
			
			//###########################################
			//Totais
			//###########################################			
						
			go_processa.m_export_totais(lt_res);
			
			//lt_res.unpersist();
						
			//###########################################						
			//Kmeans DDoS
			//###########################################
			
			lo_kmeans = new cl_kmeans(gc_stamp, gv_stamp);
			
			go_select.m_conf_phoenix(gc_kmeans_ddos, gv_session);
						
			lt_res = go_select.m_select_LogKmeans(lv_stamp);
									
			lo_kmeans.m_export_kmeans_ddos(lt_res);
			
			//lt_res.unpersist();
											
			//###########################################
			//Kmeans Port Scan
			//###########################################
			
			go_select.m_conf_phoenix(gc_kmeans_scan, gv_session);
			
			lt_res = go_select.m_select_LogKmeans(lv_stamp);
						
			lo_kmeans.m_export_kmeans_ScanPort(lt_res);
			
			//lt_res.unpersist();
			
			cl_util.m_time_end();
			
			break;
		
		case 9:
			
			go_select.m_conf_phoenix("LOG", gv_session);
			
			//utilizar as colunas das outras familias mesmo? alterado o nome da coluna add numero 1
			gt_data = go_select.m_seleciona(gc_stamp);
							
			cl_util.m_show_dataset(gt_data, "NEW");
			
			break;			
			
		}		
				
	}
	
	public static void m_conf_spark(){
		
		if(gv_submit == 0) {			
			gv_conf = new SparkConf().setMaster("local[4]").setAppName("SelectLog");
		}else {
			gv_conf = new SparkConf().setAppName("ProcessaIp");//se for executar no submit
		}
				
		gv_context = new SparkContext(gv_conf);
		
		gv_session = new SparkSession(gv_context);		
		
		Logger.getRootLogger().setLevel(Level.ERROR);
	}

}
