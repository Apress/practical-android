<?php
class DB_Functions {
    private $db;
    function __construct() {
        include_once $_SERVER['DOCUMENT_ROOT'] . '/fcm/db_connect.php';
        $this->db = new DB_Connect();
        $this->db->connect();
    }
    function __destruct() {        
    }

    public function storeUser($name, $token) {
        // insert into DB
        $result = mysql_query("INSERT INTO fcm_users(name, fcm_token, created_at) VALUES('$name', '$token', NOW())");
        // check for successful
        if ($result) {
            // get user details
            $id = mysql_insert_id();
            $result = mysql_query("SELECT * FROM fcm_users WHERE id = $id") or die(mysql_error());
            // return user details
            if (mysql_num_rows($result) > 0) {
                return mysql_fetch_array($result);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public function getAllUsers() {
        $result = mysql_query("select * FROM fcm_users");
        return $result;
    }
}
?>