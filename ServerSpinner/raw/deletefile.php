<?php

  $file_to_delete = $_POST['inputfile'];
  $file_path = $_SERVER['DOCUMENT_ROOT'] . "/pics/" . $file_to_delete;

  // build the list of files in the directory which can be deleted
  $i = 0;
  if ($handle = opendir('..')) {
    while (false !== ($file = readdir($handle))) {
      if ($file != "." && 
          $file != ".." && 
          $file != "phpfiles" &&
          $file != "listfiles-a.php" &&
          $file != "listfiles.php" && 
          $file != "uploader.php" &&
          $file != "deletefile.html" &&
          $file != "return204.php" &&
          $file != "uploadfile.html") {
            $files[$i] = $file;
            $i = $i + 1;
          }
    }
    closedir($handle);
  }

  // only allow the delete if the file is in the list of files in the directory
  $check = str_replace($files, '****', $file_to_delete, $count);
  if($count > 0) {
    if(unlink(getcwd() . "/" . $file_to_delete)) echo "File Deleted.";
  } 
  echo $file_to_delete;
  echo " ";
  echo getcwd() . "/" . $file_to_delete;
  echo " ";
  echo $file_path;  
?>