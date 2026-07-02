# yudao-module-amazon-common

Amazon Common - shared utilities for all Amazon business modules.

## Sub-modules

| Module | Description |
|--------|-------------|
| `amazon-common-core` | Core utilities: TokenStore, SpApiClient base, RateLimiter, retry policies |
| `amazon-common-model` | Shared data models: common DTOs, VOs, DOs, enums, constants |
| `amazon-common-sync` | Sync framework: Job base classes, MQ config, delta sync support |
| `amazon-common-encrypt` | AES-256 encryption for seller credentials (LWA tokens, AWS keys) |
