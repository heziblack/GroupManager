package org.hezistudio.klaxonModels

class ApexInfo (
    val global: Global,
    val realTime: RealTime,
    val legends: Legends
)

/**排位信息
 * @param rankScore 排位分数
 * @param rankName 排位等级名
 * @param rankDiv 段位内分级
 * @param ladderPosPlatform 段位平台
 * @param rankImg 段位图片
 * @param rankedSeason 赛季名
 * */
class RankAndArena(
    val rankScore:Int,
    val rankName:String,
    val rankDiv:Int,
    val ladderPosPlatform:Int,
    val rankImg:String,
    val rankedSeason:String
)

/**封禁信息
 * @param isActive 是否封禁中
 * @param remainingSeconds 剩余时间：秒
 * @param last_banReason 上次封禁理由*/
class Ban(
    val isActive:Boolean,
    val remainingSeconds:Long,
    val last_banReason:String
)

/**赛季通行证
 * @param level 当前赛季通行证等级
 * @param history 历史赛季通行证
 * */
class BattlePass(
    val level: String,
    val history: Map<String,Int>
)

/**角色全局信息
 * @param name 用户昵称
 * @param uid 用户ID
 * @param platform 用户平台
 * @param level 等级
 * @param toNextLevelPercent 下个等级所需经验百分比
 * @param bans 封禁信息
 * @param rank 大逃杀排位
 * @param arena 竞技场排位
 * @param battlePass 赛季通行证
 * @param internalParsingVersion 内部版本
 * @param badges 徽章
 * @param levelPrestige 等级声望
 * @param avatar 头像
 * @param internalUpdateCount 内部升级计数
 * */
class Global(
    val name:String,
    val uid:Long,
    val avatar:String,
    val platform:String,
    val level:Int,
    val toNextLevelPercent:Int,
    val internalUpdateCount:Int,
    val internalParsingVersion:Int,
    val badges:List<Badge>,
    val levelPrestige:Int,
    val bans:Ban,
    val rank:RankAndArena,
    val arena: RankAndArena,
    val battlePass: BattlePass
)

/**实时信息
 * @param lobbyState 大厅状态
 * @param isOnline 是否在线
 * @param isInGame 是否在游戏中
 * @param canJoin 是否可加入
 * @param partyFull 组队是否满员
 * @param selectedLegend 当前传奇
 * @param currentState 当前状态
 * @param currentStateSinceTimestamp 当前状态时间戳
 * @param currentStateAsText 当前状态文本
 * */
class RealTime(
    val lobbyState:String,
    val isOnline:Int,
    val isInGame:Int,
    val canJoin:Int,
    val partyFull:Int,
    val selectedLegend:String,
    val currentState:String,
    val currentStateSinceTimestamp:Int,
    val currentStateAsText:String
)

/**传奇*/
class Legends(
    val selected:Selected
)
/**当前选中传奇*/
class Selected(
    val LegendName:String,
    val data:List<LegendsSelectedData>,
    val gameInfo: LegendsSelectedGameInfo,
    val ImgAssets:ImgAssets
)

/**当前传奇数据*/
class LegendsSelectedData(
    val name:String,
    val value:Long,
    val key:String,
    val global:Boolean
)

/**当前传奇游戏信息*/
class LegendsSelectedGameInfo(
    val skin:String,
    val skinRarity:String,
    val frame:String,
    val frameRarity:String,
    val pose:String,
    val poseRarity:String,
    val intro:String,
    val introRarity:String,
    val badges:List<Badge>
)

/**徽章*/
class Badge(
    val name:String,
    val value: Long,
    val category:String
)

/**图像资源*/
class ImgAssets(
    val icon:String,
    val banner:String
)