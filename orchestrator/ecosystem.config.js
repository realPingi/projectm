// ecosystem.config.js
module.exports = {
  apps: [
    {
      name: "projectm-orchestrator",
      script: "./simple-orchestrator.js",
      cwd: __dirname,

      exec_mode: "fork",      // <— wichtig: kein cluster mode
      instances: 1,           // nur ein Prozess

      autorestart: true,
      watch: false,
      max_memory_restart: "200M",

      // Logs
      merge_logs: true,       // <— ein gemeinsames Logfile (keine -0, -1 Suffixe)
      error_file: "/logs/projectm-orch.err.log",
      out_file:   "/logs/projectm-orch.out.log",
      log_date_format: "YYYY-MM-DD HH:mm:ss",

      node_args: "--trace-uncaught --unhandled-rejections=strict",

      env: {
        NODE_ENV: "production",
        ORCH_PORT: 4000,
        ORCH_HOST: "0.0.0.0",
        DOCKER_BIN: "/usr/bin/docker",
        GAME_PING_HOST: "172.17.0.1",
        READY_TIMEOUT_MS: 60000,
        PING_TIMEOUT_MS: 1500,
      },
    },
  ],
};
