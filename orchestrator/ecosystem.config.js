module.exports = {
  apps: [
    {
      name: "projectm-orchestrator",
      script: "./simple-orchestrator.js",
      cwd: __dirname,              // Arbeitsverzeichnis = dieser Ordner
      instances: 1,                // nur ein Orchestrator
      autorestart: true,           // automatisch neustarten bei Crash
      watch: false,                // kein Watch-Modus in Prod
      max_memory_restart: "200M",  // Memory-Limit, optional
      env: {
        NODE_ENV: "production",
        ORCH_PORT: 4000,
        ORCH_HOST: "0.0.0.0",
        DOCKER_BIN: "/usr/bin/docker"
      },
      error_file: "/var/log/projectm-orch.err.log",
      out_file: "/var/log/projectm-orch.out.log",
      log_date_format: "YYYY-MM-DD HH:mm:ss",
    }
  ]
};