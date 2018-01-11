<?php

	$i = 0;

 	if ($handle = opendir('.')) {
  	while (false !== ($file = readdir($handle))) {
    	if ($file != "." && 
          $file != ".." && 
          $file != "listfiles-a.php" &&
          $file != "listfiles.php" && 
          $file != "phpfiles" &&
          $file != "deletefile.php" &&
          $file != "uploadfile.php" &&
          $file != "return204.php" &&
          $file != "deletefile.html" &&
          $file != "uploadfile.html") {
         		$thelist = $thelist.$file." ";
						$files[$i] = $file;
						$i = $i + 1;
          }
		}
  	closedir($handle);
  }

	// below to display the file unsorted
	//echo $thelist;
	
	// sort the files alphabetically
	asort($files);
	foreach ($files as $a) {echo $a." "; }

?>