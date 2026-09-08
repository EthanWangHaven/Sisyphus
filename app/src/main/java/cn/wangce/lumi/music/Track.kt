package cn.wangce.lumi.music

// 一首曲目：在线音源 url + 展示名 + 歌手 + assets/covers/ 封面（封面仍随包，音频走网络）
data class Track(
    val fileName: String,   // 展示用标识（历史字段）
    val title: String,
    val artist: String = "",
    val cover: String = "",
    val url: String = "",   // 在线 mp3 直链；为空则回退 assets/music/ 本地文件
)

// 内置歌单：与个人网站 EthanWangHaven 歌单一致（音频改为在线获取以减小 APK 体积；封面取自网易云专辑图）
// 音源说明：url 指向个人站点（GitHub Pages）/audio/ 下的真实音频，文件名与 fileName 一一对应，
// 替换为自建 CDN 或其他直链时只需改各 Track 的 url 字段
object Playlist {
    const val ASSET_DIR = "music"
    const val COVER_DIR = "covers"
    private const val SRC = "https://ethanwanghaven.github.io/audio"

    val tracks: List<Track> = listOf(
        Track("banjuzaijian.mp3", "半句再见", "孙燕姿", "$COVER_DIR/banjuzaijian.jpg", "$SRC/banjuzaijian.mp3"),
        Track("cry-for-me.mp3", "Cry for me", "Michita", "$COVER_DIR/cry-for-me.jpg", "$SRC/cry-for-me.mp3"),
        Track("best-time.mp3", "最好的时光", "安溥", "$COVER_DIR/best-time.jpg", "$SRC/best-time.mp3"),
        Track("marry-top-barry.mp3", "Marry Top Barry", "INDEcompany", "$COVER_DIR/marry-top-barry.jpg", "$SRC/marry-top-barry.mp3"),
        Track("give-you-give-me.mp3", "给你给我", "毛不易", "$COVER_DIR/give-you-give-me.jpg", "$SRC/give-you-give-me.mp3"),
        Track("buyao-shuohua.mp3", "不要说话", "陈奕迅", "$COVER_DIR/buyao-shuohua.jpg", "$SRC/buyao-shuohua.mp3"),
        Track("ni-shenbian.mp3", "在你的身边 (伴奏)", "盛哲", "$COVER_DIR/ni-shenbian.jpg", "$SRC/ni-shenbian.mp3"),
        Track("guli.mp3", "我离孤单几公里", "华晨宇", "$COVER_DIR/guli.jpg", "$SRC/guli.mp3"),
        Track("xiangyang.mp3", "向阳而生", "华晨宇", "$COVER_DIR/xiangyang.jpg", "$SRC/xiangyang.mp3"),
        Track("sinian.mp3", "思念是一种病 (live)", "张震岳", "$COVER_DIR/sinian.jpg", "$SRC/sinian.mp3"),
        Track("483378334.mp3", "浴室", "deca joins", "$COVER_DIR/483378334.jpg", "$SRC/483378334.mp3"),
        Track("1313588198.mp3", "Yellow", "Amber Leigh Irish", "$COVER_DIR/1313588198.jpg", "$SRC/1313588198.mp3"),
        Track("2098103269.mp3", "奇遇记", "生鱼片", "$COVER_DIR/2098103269.jpg", "$SRC/2098103269.mp3"),
        Track("2717670823.mp3", "爱情讯息", "汪珂楠", "$COVER_DIR/2717670823.jpg", "$SRC/2717670823.mp3"),
        Track("417859220.mp3", "皆非", "马頔", "$COVER_DIR/417859220.jpg", "$SRC/417859220.mp3"),
        Track("2629720361.mp3", "爱错", "刘大拿", "$COVER_DIR/2629720361.jpg", "$SRC/2629720361.mp3"),
        Track("mtpxranhxi2q.mp3", "多远都要在一起", "邓紫棋", "$COVER_DIR/mtpxranhxi2q.jpg", "$SRC/mtpxranhxi2q.mp3"),
        Track("mtpxtmlyzv29.mp3", "唯一", "邓紫棋", "$COVER_DIR/mtpxtmlyzv29.jpg", "$SRC/mtpxtmlyzv29.mp3"),
        Track("102193382.mp3", "达尔文", "蔡健雅", "$COVER_DIR/102193382.jpg", "$SRC/102193382.mp3"),
        Track("104882784.mp3", "失语者", "蔡健雅", "$COVER_DIR/104882784.jpg", "$SRC/104882784.mp3"),
        Track("247262095.mp3", "带我去找夜生活 - 健康版", "告五人", "$COVER_DIR/247262095.jpg", "$SRC/247262095.mp3"),
        Track("496054946.mp3", "恋人", "李荣浩", "$COVER_DIR/496054946.jpg", "$SRC/496054946.mp3"),
        Track("231911714.mp3", "曾经是情侣", "梁博", "$COVER_DIR/231911714.jpg", "$SRC/231911714.mp3"),
        Track("231594573.mp3", "出现又离开", "梁博", "$COVER_DIR/231594573.jpg", "$SRC/231594573.mp3"),
        Track("233814890.mp3", "我不知道", "梁博", "$COVER_DIR/233814890.jpg", "$SRC/233814890.mp3"),
        Track("206713449.mp3", "极美", "孙燕姿", "$COVER_DIR/206713449.jpg", "$SRC/206713449.mp3"),
        Track("5646.mp3", "开始懂了", "孙燕姿", "$COVER_DIR/5646.jpg", "$SRC/5646.mp3"),
        Track("5211222.mp3", "天黑黑", "孙燕姿", "$COVER_DIR/5211222.jpg", "$SRC/5211222.mp3"),
        Track("8136.mp3", "我不难过", "孙燕姿", "$COVER_DIR/8136.jpg", "$SRC/8136.mp3"),
        Track("290330693.mp3", "忘了没有", "王靖雯", "$COVER_DIR/290330693.jpg", "$SRC/290330693.mp3"),
        Track("200549978.mp3", "朵", "赵雷", "$COVER_DIR/200549978.jpg", "$SRC/200549978.mp3"),
        Track("102065756.mp3", "七里香", "周杰伦", "$COVER_DIR/102065756.jpg", "$SRC/102065756.mp3"),
        Track("97773.mp3", "晴天", "周杰伦", "$COVER_DIR/97773.jpg", "$SRC/97773.mp3"),
        Track("370870467.mp3", "夜的尽头", "邓紫棋", "$COVER_DIR/370870467.jpg", "$SRC/370870467.mp3"),
        Track("1868184523.mp3", "我们被夹在云层之间", "Vicky宣宣", "$COVER_DIR/1868184523.jpg", "$SRC/1868184523.mp3"),
        Track("1817160410.mp3", "三十二段留言", "Vicky宣宣", "$COVER_DIR/1817160410.jpg", "$SRC/1817160410.mp3"),
        Track("1914369550.mp3", "越过狂风暴雨奔向你", "Vicky宣宣", "$COVER_DIR/1914369550.jpg", "$SRC/1914369550.mp3"),
        Track("2005246398.mp3", "一屏之念", "Vicky宣宣", "$COVER_DIR/2005246398.jpg", "$SRC/2005246398.mp3"),
        Track("1399123309.mp3", "Miyazaki Mountain", "Philter", "$COVER_DIR/1399123309.jpg", "$SRC/1399123309.mp3"),
        Track("mtqwdtts6f88.mp3", "我的天空", "南征北战NZBZ", "$COVER_DIR/mtqwdtts6f88.jpg", "$SRC/mtqwdtts6f88.mp3"),

    )
}
