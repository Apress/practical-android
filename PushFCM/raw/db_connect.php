<?php
class DB_Connect {
    function __construct() {
    }
    function __destruct() {
    }
    public function connect() {
 				define("DB_HOST", "localhost");
				define("DB_USER", "fcm_user1");
				define("DB_PASSWORD", "mJw$31");
				define("DB_DATABASE", "fcm");

        $con = mysql_connect(DB_HOST, DB_USER, DB_PASSWORD);
        mysql_select_db(DB_DATABASE);
        return $con;
    }
    public function close() {
        mysql_close();
    }
} 
?>