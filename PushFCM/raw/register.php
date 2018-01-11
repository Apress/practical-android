<?php

if (isset($_POST['name']) && isset($_POST['token'])) {
    $name = $_POST['name'];
    $token = $_POST['token'];

    include_once $_SERVER['DOCUMENT_ROOT'] . "/fcm/db_functions.php";
    include_once $_SERVER['DOCUMENT_ROOT'] . "/fcm/FCM.php";

    $db = new DB_Functions();
    $fcm = new FCM();

    $res = $db->storeUser($name, $token);
    //$tokens = array($fcm_token);
    //$message = array("message" => "Registering on FCM");
    $result = $fcm->send_notification($token, $name);
    echo $result;
}

fclose($handle);

?>