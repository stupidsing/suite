source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/cache.sh)

BASE=$(cchs "echo git@github.com:stupidsing/suite.git" @git-clone "@git-cd pwd")

${BASE}/build.sh

printf "[Service]
ExecStart=${BASE}/service.sh
User=root
WorkingDirectory=${BASE}

SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
" | sudo tee /etc/systemd/system/suite.service

echo -e "#\x21/bin/sh
${JAVA_HOME}/bin/java -Dsuite.dir=${BASE} -cp \$(cat ${BASE}/target/classpath):${BASE}/target/suite-1.0.jar suite.ServerMain
" > ${BASE}/service.sh

chmod 755 ${BASE}/service.sh

sudo systemctl daemon-reload
sudo systemctl enable suite.service
sudo systemctl start suite

#sudo systemctl status suite
#sudo journalctl -u suite
#sudo journalctl -f -u suite
#curl http://localhost:8051/sse

#sudo systemctl stop suite
#sudo systemctl disable suite.service
