





#### 2\. Stopping the service

systemctl stop protel-api





#### 3\. Backup the current jar (just in case)



mv /opt/protel-api/app.jar /opt/protel-api/app.jar.bak





#### 4.Replace the old jar with the new jar



mv /opt/protel-api/app.jar.new /opt/protel-api/app.jar



#### 5.Fix permissions

chown protelapi:protelapi /opt/protel-api/app.jar







#### 6.Start the service again



systemctl start protel-api







#### 7.Confirm it’s running



systemctl status protel-api --no-pager







If something goes wrong (rollback in 10 seconds

systemctl stop protel-api

mv /opt/protel-api/app.jar  /opt/protel-api/app.jar.bad

mv /opt/protel-api/app.jar.bak  /opt/protel-api/app.jar

chown protelapi:protelapi  /opt/protel-api/app.jar

systemctl start protel-api















