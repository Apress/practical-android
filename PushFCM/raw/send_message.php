<?php

if (isset($_GET["token"]) && isset($_GET["message"])) {
    $token = $_GET["token"];
    $message = $_GET["message"];
    
    include_once $_SERVER['DOCUMENT_ROOT'] . '/fcm/FCM.php';
    
    $fcm = new FCM();

    //$tokens = array($token);
    //$message = array("message" => $message);

    $result = $fcm->send_notification($token, $message);

    echo $result;
}
?>
