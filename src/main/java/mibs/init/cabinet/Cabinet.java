package mibs.init.cabinet;

public interface Cabinet {

	String CMD_INIT_CABINET 			= "INIT";
	String CMD_ADD_CONCLUSION 			= "ADD_CONCLUSION";
	String CMD_ADD_EXPLORATION			= "ADD_EXPLORATION";
	String CMD_PROLONG_CABINET 			= "PROLONG";
	String CMD_BLOCK_CABINET 			= "BLOCK";
	
	
	String CMD_INITIALIZED 				= "INITIALIZED";
	String CMD_NOT_INITIALIZED 			= "NOT_INITIALIZED";
	String CMD_CONCLUSION_ADDED			= "CONCLUSION_ADDED";
	String CMD_CONCLUSION_NOT_ADDED 	= "CONCLUSION_NOT_ADDED";
	String CMD_EXPLORATION_ADDED		= "EXPLORATION_ADDED";
}
