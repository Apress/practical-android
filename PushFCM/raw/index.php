<?php
    echo "<script src='https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js'></script>
          <script type='text/javascript'>
          $(document).ready(function(){
          });
          
          function sendPushNotification(id){
              var data = $('form#'+id).serialize();
              $('form#'+id).unbind('submit');                
              $.ajax({
                  url: 'http://www.majawi.com/fcm/send_message.php',
                  headers: {
                        'Access-Control-Allow-Origin' : '*',
                        'Access-Control-Allow-Methods' : 'GET, POST, PUT, DELETE, OPTIONS'
                  },
                  type: 'GET',
                  data: data,
                  beforeSend: function() {  
                  },
                  success: function(data, textStatus, xhr) {
                        $('.txt_message').val('');
                  },
                  error: function(xhr, textStatus, errorThrown) {
                  }
              });
              return false;
          }
          </script>
        
          <style type='text/css'>
              .containerA{
                  width: 80%;
                  margin: 0 auto;
                  padding: 0;
              }
              h1{
                  font-family: 'Arial', Helvetica, Arial, sans-serif;
                  font-size: 16px;
                  color: #777;
              }
              div.clear{
                  clear: both;
              }
              ul.devices{
                  margin: 0;
                  padding: 0;
              }
              ul.devices li{
                  float: left;
                  list-style: none;
                  padding: 5px;
                  margin: 0 0 5px 0;
                  font-family: 'Arial', Helvetica, Arial, sans-serif;
                  color: #555;
              }
              ul.devices li label, ul.devices li span{
                  font-family: 'Arial', Helvetica, Arial, sans-serif;
                  font-size: 12px;
                  font-style: normal;
                  font-variant: normal;
                  font-weight: bold;
                  color: #333;
                  display: block;
                  float: left;
              }
              ul.devices li label{
                  height: 15px;
                  width: 120px;                
              }
              ul.devices li textarea{
                  float: left;
                  resize: none;
              }
              ul.devices li .send_btn{
                  background-color: #fff;
                  border-radius: 4px;
                  color: #333;
              }
          </style>";

    include_once $_SERVER['DOCUMENT_ROOT'] . '/fcm/db_functions.php';
    $db = new DB_Functions();
    $users = $db->getAllUsers();
    if ($users != false)
        $no_of_users = mysql_num_rows($users);
    else
        $no_of_users = 0;
    ?>

    <div class="containerA">
        <h3>FCM Push Message Demo&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Number of registered devices = <?php echo $no_of_users; ?></h3>
        <ul class="devices">
            <?php if ($no_of_users > 0) {?>
                <table>
                <?php while ($row = mysql_fetch_array($users)) {?>
                    <tr><td><hr/></td></tr>
                    
                    <tr><td>
                    <li>
                        <form id="<?php echo $row["id"] ?>" name="" method="post" onsubmit="return sendPushNotification('<?php echo $row["id"] ?>')">
                            <label>Id:</label><span><?php echo $row["id"] ?> </span>  
                            <div class="clear"></div>
                            
                            <label>Name:</label><span><?php echo $row["name"] ?></span>
                            <div class="clear"></div>
                            
                            <label>Token:</label><span><?php echo $row["fcm_token"] ?></span>
                            <div class="clear"></div>

                            <div class="send_container">                          
                                <textarea rows="1" name="message" cols="200" placeholder="Push message ..."></textarea>
                            </div>
                            <div class="clear"></div>
                            
                            <input type="submit" class="send_btn" value="Send" onclick=""/>
                        </form>
                    </li>
                    </td></tr>
                    
                <?php }
            } else { ?> 
                <li>No users currently registered</li>
            <?php } ?>
        </ul>
      </table>
    </div>
    </body>
</html>
