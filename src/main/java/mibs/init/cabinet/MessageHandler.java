package mibs.init.cabinet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.Channel;

public abstract class MessageHandler implements Cabinet{
	private static final Logger logger = LogManager.getLogger(MessageHandler.class.getName());
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss d MMM uuuu");
	
	protected String rabbitmqHost;
	protected String rabbitmqLogin;
	protected String rabbitmqPassword;
	
	protected String directExchange;
	protected String responceQueue;
	protected String downstreamQueue;
	
	protected String conclusionPath ;
	protected String dicomPath ;
	protected String serializedPath ;
	
	protected Channel channel = null;
	
	
	protected Map< String, Consumer< RabbitmqCommandMessage< ? > > > commands ;
	
	
	protected  void initConfig(String conf) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(conf)) {
			props.load(fis);
			rabbitmqHost 		= props.getProperty("rabbitmq-host");
			rabbitmqLogin 		= props.getProperty("rabbitmq-login");
			rabbitmqPassword	= props.getProperty("rabbitmq-password");
			directExchange 		= props.getProperty("direct-exchange");
			responceQueue 		= props.getProperty("responce-queue");
			downstreamQueue 	= props.getProperty("downstream-queue");
			conclusionPath		= props.getProperty("conclusion-storage-path");
			dicomPath 			= props.getProperty("dicom-storage-path");
			serializedPath		= props.getProperty("serialized-storage-path");
			
			logger.trace("Application started at " + formatter.format(LocalDateTime.now()));
		}
	}

	public MessageHandler(String conf) {
		
		try {
			initConfig(conf);
		} catch (FileNotFoundException e) {
			logger.error("Error! Configuration file not found!");
			exit();
		} catch (IOException e) {
			logger.error("Error! IO Exception with message: " + e.getMessage());
			exit();
		}
		
		commands = new TreeMap<>();
		commands.put(CMD_INIT_CABINET, ( u ) -> {
			try {
				InitNewCabinet(u);
				logger.trace("Publish to exchange " + directExchange + " message:  " + u);
			} catch (IOException e) {
				logger.error("Error! Publish to queue localin failed with message: " + e.getMessage());
			}
		});
		
		commands.put(CMD_ADD_CONCLUSION, ( u ) -> {
			try {
				addConclusion(u);
				logger.trace("Publish to exchange " + directExchange + " message:  " + u);
			} catch (IOException e) {
				logger.error("Error! Publish to queue localin failed with message: " + e.getMessage());
			}
		});
		commands.put(CMD_ADD_EXPLORATION, ( u ) -> {
			try {
				addExploration( u ) ;
				logger.trace("Publish to exchange " + directExchange + " message:  " + u);
			} catch (IOException e) {
				logger.error("Error! Publish to queue localin failed with message: " + e.getMessage());
			}
		});
	}
	private void addExploration( RabbitmqCommandMessage<?> u ) throws IOException {
		boolean isExploratioAdded = true;
		String routingKey = u.getRoutingKey();
		Exploration exploration = ( Exploration ) u.getContent();
		if ( isExploratioAdded ) {
			RabbitmqCommandMessage<Exploration> result = new RabbitmqCommandMessage<>( CMD_EXPLORATION_ADDED,exploration );
			channel.basicPublish( directExchange, routingKey, true, null, SerializationUtils.serialize ( result ) );

		}
	}
	private void InitNewCabinet(RabbitmqCommandMessage<?> u) throws IOException{
		Person p = (Person)u.getContent();
		if (!findPerson(p)) {
			String routingKey = u.getRoutingKey();
			RabbitmqCommandMessage<Person> result = new RabbitmqCommandMessage<>(CMD_INITIALIZED, p);
			byte[] rc = SerializationUtils.serialize ( result );
			channel.basicPublish( directExchange, routingKey, true, null, rc );
			
		}
	}
	private void addConclusion(RabbitmqCommandMessage<?> u) throws IOException {
		
		boolean isConclusionAdded = true;
		Conclusion conclusion = (Conclusion) u.getContent();
		String routingKey = u.getRoutingKey();
		
		String _path = conclusionPath + "/" + conclusion.getUniqueID();
		Files.createDirectories( Paths.get(_path) );
		String fileName = _path + "/" + conclusion.getConclusionName();
		FileUtils.writeByteArrayToFile(new File(fileName), conclusion.getConclusionContent());
		
		if(isConclusionAdded) {
			
			RabbitmqCommandMessage<Conclusion> result = new RabbitmqCommandMessage<>(CMD_CONCLUSION_ADDED,
																					 new Conclusion(null, conclusion.getConclusionName(), null, null ));
			channel.basicPublish( directExchange, routingKey, true, null, SerializationUtils.serialize ( result ) );
		}else {
			
			RabbitmqCommandMessage<Conclusion> result = new RabbitmqCommandMessage<>(CMD_CONCLUSION_NOT_ADDED,
																					new Conclusion(null, conclusion.getConclusionName(), null, null ));
			channel.basicPublish( directExchange, routingKey, true, null, SerializationUtils.serialize ( result ) );	
		}
		
	}
	private boolean findPerson(Person p){
		return false;
	}
	protected static void exit() {
		logger.info("Application exit at " + formatter.format(LocalDateTime.now()));
		System.exit(0);
	}
}
