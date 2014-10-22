import java.io.*;
import java.util.*;


public class LSA {

	public static void main(String[] args) throws IOException {
		String base_path = LSA.class.getResource("/") .getPath() ;
		String Query = base_path + "QUERY_WDID_NEW" ;
		String Docuementset = base_path + "SPLIT_DOC_WDID_NEW" ;
		int dimention = 130 ; //dim
		String S = base_path + "LSA130-S" ;
		String Ut = base_path + "LSA130-Ut" ;
		String Vt = base_path + "LSA130-Vt" ;
		String IDF = base_path + "WD-IDF-List" ;
		String nextline = null ;
		Double[][] trans_Vt = new Double[2265][dimention] ; 
		Hashtable<Integer,String[]> trans_Ut = new  Hashtable<Integer,String[]>() ; 
		File tQuery = new File(Query) ;
		String[] Query_name = tQuery.list() ;
		Double[] temp_IDF = new Double[51253] ; //存入WD-IDF-List
		Double[] query_TF_IDF ; //當前該query的tf_idf
		Double[] eigenvalue = new Double[dimention] ;
		
		
		
		Scanner wIDF;
		Scanner S_reverse ;
		Scanner docuement_V ;
		Scanner Ufile ;
		
		File tdata = new File(Docuementset) ;
		String[] docuement_name_list = tdata.list() ;

		try { //load IDF
			wIDF = new Scanner(new File(IDF));  //51253 = 0~51252 term
			
			while(wIDF.hasNext()){
				nextline = wIDF.nextLine() ;
				String[] word_IDF = nextline.split(" ") ;
				int lastIndex = word_IDF.length-1 ;
				
				try{ 
					temp_IDF[Integer.valueOf(word_IDF[lastIndex-1])] = Double.valueOf(word_IDF[lastIndex]) ;
				}
				catch (NumberFormatException e){ 
					System.out.println(" parse double error!! " + e);
				}
			}
			
			//反矩陣S dimention
			wIDF.close() ;
			
			S_reverse = new Scanner(new File(S));
			boolean oneline = false ;
			int index = 0 ;

			while(S_reverse.hasNext()){
				if(!oneline){
					nextline = S_reverse.nextLine() ; //first line
					oneline = true ;
				}
				nextline = S_reverse.nextLine() ;
				eigenvalue[index] = 1/(Double.valueOf(nextline)) ;
				index++ ;
			}
			
			S_reverse.close() ;
			
			//Vt轉至V並降維
			docuement_V = new Scanner(new File(Vt));  //51253 = 0~51252 term
			oneline = false ;
			index = 0 ;
			while(docuement_V.hasNext()){
				if(!oneline){
					nextline = docuement_V.nextLine() ; //first line
					oneline = true ;
				}
				
				nextline = docuement_V.nextLine() ;
				String[] Vc = nextline.split(" ") ;
				for(int vlen=0;vlen<Vc.length;vlen++){
					trans_Vt[vlen][index]= Double.valueOf(Vc[vlen]) ;
				}
				index++ ;
			}
			docuement_V.close();
			//load Ut
			Ufile = new Scanner(new File(Ut)) ;
			oneline = false ;
			index = 0 ;
			while(Ufile.hasNext()){
				if(!oneline){
					oneline = true ;
					Ufile.nextLine() ;//下一行
				}
				String[] n_Ut = Ufile.nextLine().split(" ") ;
				trans_Ut.put(index, n_Ut) ;
				index++ ;
				
			}
			Ufile.close() ;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		FileWriter output = new FileWriter(base_path + "outlist130.txt" );
		PrintWriter out = new PrintWriter(output) ;
		String line = null ;
		//Query_name.length
		for(int i=0;i<Query_name.length;i++){ //當前query_1~16  key0~51252
			query_TF_IDF = new Double[51253] ;
			Scanner queryfile = new Scanner(new File(Query+"/"+Query_name[i])) ; //open query
			Hashtable<String,Integer> query_TF = new  Hashtable<String,Integer>() ;//當前query_TF
			
			while(queryfile.hasNext()){
				line = queryfile.nextLine() ;//下一行
				String[] querydata = line.split(" ") ;
				
				for(int t=0;t<querydata.length;t++){
					if(!querydata[t].equals("-1")){ //count query.txt 中的TF值
						if(query_TF.containsKey(querydata[t])){
							int count = query_TF.get(querydata[t]) ;
							count ++ ;
							query_TF.put(querydata[t],count) ;
						}
						else{
							query_TF.put(querydata[t], 1) ;
						}
					}
				}
			}
			
			if(i==1){
				System.out.print("");
			}
			//query_TF * query_IDF
			Enumeration index = query_TF.keys() ;
			String key = null ;
			int term = 0 ;
			int value = 0 ;
			
			
			while(index.hasMoreElements()) {
				key = (String)index.nextElement() ;
				term = query_TF.get(key) ; //tf值 key0~51252
				value = Integer.valueOf(key) ;
				query_TF_IDF[value] = temp_IDF[value] * term ;
				//System.out.println(key+":"+query_TF_IDF[value]) ;
			}
			
			//q = qt*Uk*S-上面沒問題
			double[] q = new double[dimention] ; 
			boolean oneline = false ;
			double sum = 0 ;
			
			for(int k=0;k<dimention;k++){ //矩陣乘法
				String row[] = trans_Ut.get(k) ;
				for(int l=0;l<row.length;l++){
					if(query_TF_IDF[l]!=null)
						sum = sum + query_TF_IDF[l] * Double.valueOf(row[l]) ;
				}
				q[k] = sum * eigenvalue[k];
				sum = 0 ;
			}
			/*
			for(int q_index=0;q_index<q.length;q_index++){
				System.out.println(q_index+":"+q[q_index]);
			}
			*/
			
			//similar q vs docuement 上面沒問題了
			double[] list = new double[2265] ;
			
			for(int l=0;l<list.length;l++){ 
				double dot = 0 ;
				double vt_value = 0 ;
				double x_length = 0 ;
				double y_length = 0 ;
				for(int k=0;k<q.length;k++){
					vt_value = trans_Vt[l][k] ;
					dot = dot + q[k] * vt_value ; //trans_Vt 2265 * 10
					x_length = x_length + q[k]*q[k] ;
					y_length = y_length + vt_value * vt_value ;
				}
				list[l] = dot/(Math.sqrt(x_length)*Math.sqrt(y_length)) ;
				
			}
			
			// inserting sort
			double temp ;
			int sorted ;
			String n_data ;
			for(int x = 1; x < list.length ; x++){ 
				temp = list[x] ; 
				n_data = docuement_name_list[x] ;
				sorted = x - 1 ;
				while( sorted >= 0 && list[sorted] > temp){
					list[sorted+1] = list[sorted] ;
					docuement_name_list[sorted+1] = docuement_name_list[sorted] ; 
					sorted = sorted - 1 ; 
				}
				list[sorted + 1] = temp ;
				docuement_name_list[sorted + 1] = n_data ;
				
			}
			
			//印出排序

			out.print("Query "+(i+1)+"\t"+Query_name[i]+" "+docuement_name_list.length) ;
			out.print("\r\n") ;

			for(int x=docuement_name_list.length-1,reverse=1 ; reverse < 2266 ; x--,reverse++)
			{
				out.print(docuement_name_list[x]+" "+list[x]+"\r\n") ;
			}
			out.print("\r\n") ;
			docuement_name_list = tdata.list() ;
		}
		output.close() ;
		
	}

}
