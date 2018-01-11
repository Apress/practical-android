<?php

    $fname = $_POST['filename'];
    $target_path = $_SERVER['DOCUMENT_ROOT'] . "/pics/" . $fname;
     
    $upload_path = $_FILES['uploadedfile']['tmp_name'];

    if(move_uploaded_file($upload_path, $target_path)) {
      echo "Moved";
    } else{
      echo "Not Moved";
    }

?>