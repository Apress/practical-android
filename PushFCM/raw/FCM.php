<?php
class FCM {
    function __construct() {
    }
    public function send_notification($token, $message) {

		define("FIREBASE_API_KEY", "your-api-key");
				
        $url = 'https://fcm.googleapis.com/fcm/send';
        $fields = array(
            'to' => $token,
            'data' => $message,
        );
        $headers = array(
            'Authorization: key=' . FIREBASE_API_KEY,
            'Access-Control-Allow-Origin: *',
            'Content-Type: application/json'
        );

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        $result = curl_exec($ch);
        if ($result === FALSE) {
            die('Curl failed: ' . curl_error($ch));
        }
        curl_close($ch);
        echo $result;
    }
}
?>