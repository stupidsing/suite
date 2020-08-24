################################################################################
source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/tools-path.sh | bash -)

PRIMAL=$(cchs "echo git@github.com:stupidsing/primal.git" @git-clone "@git-cd pwd")
SUITE=$(cchs "echo git@github.com:stupidsing/suite.git" @git-clone "@git-cd pwd")
SERVE=${HOME}/serve

(cd ${PRIMAL} && mvn install) &&
${SUITE}/build.sh

################################################################################
printf "
ProxyPass /ywsing http://127.0.0.1:8051/
ProxyPassReverse /ywsing http://127.0.0.1:8051/
ProxyPreserveHost On
" | sudo tee /etc/apache2/sites-enabled/000-suite.conf > /dev/null

sudo systemctl restart apache2

################################################################################
printf "[Service]
ExecStart=${SUITE}/service.sh
User=root
WorkingDirectory=${SUITE}

SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
" | sudo tee /etc/systemd/system/suite.service > /dev/null

echo -e "#\x21/bin/sh
HOME=${SERVE} ${JAVA_HOME}/bin/java -Dsuite.dir=${SUITE} -cp \$(cat ${SUITE}/target/classpath):${SUITE}/target/suite-1.0.jar suite.ServerMain
" > ${SUITE}/service.sh

chmod 755 ${SUITE}/service.sh

sudo systemctl daemon-reload

sudo systemctl enable suite.service
sudo systemctl restart suite

################################################################################
sudo systemctl status suite --no-pager
sudo journalctl -f -u suite
#curl https://pointless.online/ywsing/sse
