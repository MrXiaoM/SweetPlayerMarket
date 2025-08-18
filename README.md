# SweetPlayerMarket

Minecraft 玩家全球市场插件。

> 插件暂未正式发布，有可能会频繁作出破坏性变更，请勿将本插件用于生产环境！

## TODO

+ [x] 设计数据表
+ [x] 全球市场商品筛选菜单（首页）
+ [ ] 查看自己的已上架物品列表
+ [ ] 查看自己的历史上架记录
+ [x] 出售商店确认付款菜单
+ [ ] 收购商店确认出售菜单
+ [x] 上架物品命令
+ [ ] 上架物品菜单
+ [ ] 特定物品限制上架条件
+ [ ] 可配置的商品到期时间
+ [ ] 商品上架收取手续费
+ [ ] …更多功能

## 简介

SweetPlayerMarket 是一个支持跨服、支持数据库的玩家全球市场插件。

玩家可以在全球市场以**出售**或**收购**的方式，上架自己的物品，让其他玩家可以自由购买或出售他们的物品。

同时，你也可以限制玩家的上架行为，让某些物品禁止上架，或者只能使用某种货币上架，还可以收取上架手续费。

利用数据库丰富的查询参数，本插件允许你以丰富的条件来排序商品列表。例如，按 `创建时间`、`到期时间`、`价格`、`数量` 其中的一个数据表列名进行 `升序 (从小到大)` 或 `降序 (从大到小)` 的方式进行排序。

本插件依然沿用 [PluginBase](http://plugins.mcio.dev/elopers/base/gui-config) 的高可自定义界面引擎，允许你让商品列表以**任何奇形怪状的形式**显示在这最大 `6*9` 格子大小箱子菜单中！

## 命令与权限

根命令为 `/sweetplayermarket`，别名为 `/playermarket`, `/spm`, `/pm`  
以 `<>` 包裹的为必选参数，以 `[]` 包裹的为可选参数。

| 命令                                         | 描述             | 权限                              |
|--------------------------------------------|----------------|---------------------------------|
| 玩家命令                                       |                |                                 |
| `/pm open`                                 | 打开全球市场首页       | `sweet.playermarket.open`       |
| `/pm create <商店类型> <价格> <货币> [单个数量] [总份数]` | 上架商品到全球市场      | `sweet.playermarket.create`     |
| 管理员命令                                      |                |                                 |
| `/pm open [玩家]`                            | 为自己或某人打开全球市场首页 | `sweet.playermarket.open.other` |
| `/pm reload database`                      | 重新连接数据库        | OP/控制台                          |
| `/pm reload`                               | 重载插件配置文件       | OP/控制台                          |

+ `sweet.playermarket.create.currency.vault` 允许使用 Vault 金币上架商品
+ `sweet.playermarket.create.currency.playerpoints` 允许使用 PlayerPoints 点券上架商品
+ `sweet.playermarket.create.currency.mpoints.<点数>` 允许使用 MPoints 点数上架商品
