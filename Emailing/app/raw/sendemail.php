<?php
		$name = $_POST['name'];
		$to = $_POST['to'];
		$from = $_POST['from']; 
		$subject = $_POST['subject'];
		
		$message = "From: ".$name."\r\n";
		$message .= $_POST['message'];
		$headers = "From:" . $from;
		mail($to,$subject,$message,$headers);
?>