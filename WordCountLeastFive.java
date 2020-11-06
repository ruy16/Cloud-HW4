package wordcount;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.tools.Tool;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.Logger;


public class WordCountLeastFive extends  Mapper<Object, 
Text, Text, LongWritable> {
public static long starttime;
  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();
    private TreeMap<Integer, LinkedList<String>> tmap; 
    
    @Override
    public void setup(Context context) throws IOException, 
                                     InterruptedException 
    { 
        tmap = new TreeMap<Integer,LinkedList<String>>(); 
    } 
    
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
    
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      if (tmap.containsKey(sum)) {
    	  tmap.get(sum).add(key.toString());
      }
      else {
    	  LinkedList<String> newList = new LinkedList<String>();
    	  newList.add(key.toString());
    	  tmap.put(sum,newList);
      }
      int valueCount = 0;
      for(LinkedList<String> v : tmap.values()) {
		valueCount += v.size();
      }
	  if (valueCount > 5) {
  		for(int i=0;i<valueCount-5;i++) {
  			int lastkey = tmap.lastKey();
  			LinkedList<String> ln = tmap.get(lastkey);
  			ln.remove();
  			if (ln.size()==0) tmap.remove(lastkey);
  			else tmap.put(tmap.lastKey(),ln);  
  		}
  		 
  	 }
      //result.set(sum);
      //context.write(key, result);
      
    }
    @Override
    public void cleanup(Context context) throws IOException, InterruptedException
    { 
        for (Map.Entry<Integer, LinkedList<String>> entry : tmap.entrySet())  
        { 
            int count = entry.getKey(); 
            for (String name : entry.getValue()) {
            	context.write(new Text(name), new IntWritable(count)); 
            }
        } 
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    starttime = System.currentTimeMillis();
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCountLeastFive.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileInputFormat.addInputPath(job, new Path(args[1]));
    FileInputFormat.addInputPath(job, new Path(args[2]));
    FileOutputFormat.setOutputPath(job, new Path(args[3]));
    if (job.waitForCompletion(true)){
    	System.out.print("\nExecution time: "+(System.currentTimeMillis()-starttime) +"ms\n");
    	System.exit(0);
    }
    System.exit(1);
    
  }
}