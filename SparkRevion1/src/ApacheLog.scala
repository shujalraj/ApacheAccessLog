import org.apache.spark.streaming.{Seconds,StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.kafka.common.serialization.StringDeserializer
import kafka.serializer.StringDecoder

import org.apache.log4j.{Logger,Level}

object ApacheLog {
  def main(args: Array[String]) = {
    
Logger.getLogger("org").setLevel(Level.ERROR)
val ssc = new StreamingContext("local[*]","DDOS",Seconds(1))
val kafkaParams = Map("bootstrap.servers" -> "localhost:9092,anotherhost:9092",
                      "key.deserializer" -> classOf[StringDeserializer],
                      "value.deserializer" -> classOf[StringDeserializer],
                      "group.id" -> "use_a_separate_group_id_for_each_stream",
                      "auto.offset.reset" -> "latest",
                      "enable.auto.commit" -> (false: java.lang.Boolean)
                      )


val topics = List("testLogs").toSet
    
val stream = KafkaUtils.createDirectStream[String, String](
    ssc,
    PreferConsistent,
    Subscribe[String, String](topics, kafkaParams)
  )
  
val lines = stream.map(x => (x.value))   
val ips = lines.map(x=> x.split(" ")(0))                              
val ipsCounts = ips.map(x =>(x,1)).reduceByKeyAndWindow(_+_,_-_,Seconds(120),Seconds(1))
val badIps = ipsCounts.filter(x=> x._2>2)
val sortedResults = badIps.transform(rdd => rdd.sortBy( x=>x._2,false))
sortedResults.print()
badIps.saveAsTextFiles("C:/Users/hp/Desktop/phData/out/")
ssc.checkpoint("C:/Users/hp/Desktop/phData/")
ssc.start()
ssc.awaitTermination()

  }
  
}