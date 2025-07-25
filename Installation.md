## Installation walkthrough (development)

```bash
git clone https://github.com/Siddhu2502/ShareServer
cd ShareServer
```

thats it ! no npm install or anything, just run the server.

```bash
mvn spring-boot:run
```

now you can access your site at `http://localhost:8080`

----------------------------------------------------------------------

## Installation walkthrough (production)

Create a website folder in /var/www/<yourwebsite> and before downloading the snapshot you can run the following to create the necessary folders
then download the snapshot from the releases page.

```bash
cd /var/www/<yourwebsite>
mkdir -p files/{public,private}
``` 

then download the snapshot from the releases page and place the downloaded jar file in the `/var/www/<yourwebsite>` folder.

Create a file named `application.properties` in the `/var/www/<yourwebsite>` folder and add the following lines to it:

```bash
touch application.properties
```

```bash

spring.application.name=filesharing
# File Storage Paths
files.base-path.public= <PASTE YOUR PUBLIC PATH HERE>
files.base-path.private= <PASTE YOUR PRIVATE PATH HERE>

# Private Folder Codes
# Format: private.code.<the_code>=<folder_name>

# Example: (uncomment and modify as needed)
# private.code={'openfile4': 'folder4'}

```

**Create a systemd service file to run the server as a service:**

```bash
sudo nano /etc/systemd/system/filesharing.service
```

```bash
[Unit]
Description=FileSharing Spring Boot Application
After=network.target

[Service]
User=<your_username>
WorkingDirectory=/var/www/<yourwebsite>

ExecStart=/usr/bin/java -jar filesharing-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

⚠️ make sure the current user has write permissions to the Folder (your website in /var/www). 

and finally create a nginx configuration file to serve the website:

```bash
sudo nano /etc/nginx/sites-available/<yourwebsite>
```

```bash
server {
    server_name filesharing.serveris.live;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
	
    # if files are huge timeout may become a issue
	proxy_buffering off;
	proxy_read_timeout 300s;
	proxy_send_timeout 300s;

    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/filesharing.serveris.live/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/filesharing.serveris.live/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}
server {
    if ($host = filesharing.serveris.live) {
        return 301 https://$host$request_uri;
    } # managed by Certbot

    listen 80;
    server_name filesharing.serveris.live;
    return 404; # managed by Certbot
}
```

Now finally 

```bash
sudo certbot --nginx -d <your_website>
sudo systemctl daemon-reload
sudo systemctl start filesharing.service

# to see the status
sudo systemctl status filesharing.service
```