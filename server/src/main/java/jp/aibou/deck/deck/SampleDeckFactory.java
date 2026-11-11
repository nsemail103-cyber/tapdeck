package jp.aibou.deck.deck;

import jp.aibou.deck.model.Action;
import jp.aibou.deck.model.Deck;
import jp.aibou.deck.model.KeyDef;
import jp.aibou.deck.model.PageDef;
import jp.aibou.deck.model.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
* The deck seeded on first run: three pages (main / study / phrases)
* shaped by a five-AI consultation round on 2026-11-11 and the owner's
* own first taps. Deliberately leaves page 3 partly empty so keys that
* prove unnecessary can be swapped out instead of crowding the grid.
*/
@Component
public class SampleDeckFactory {

public Deck sampleDeck() {
PageDef main = new PageDef("page-main", "メイン", 3, 5, List.of(
new KeyDef("key-save", 0, 0, "保存", KeyDef.Icon.emoji("💾"),
new Action.Hotkey(List.of("CTRL"), "S")),
new KeyDef("key-snip", 0, 1, "スクショ", KeyDef.Icon.emoji("✂️"),
new Action.Hotkey(List.of("WIN", "SHIFT"), "S")),
new KeyDef("key-clipboard-history", 0, 2, "履歴", KeyDef.Icon.emoji("📋"),
new Action.Hotkey(List.of("WIN"), "V")),
new KeyDef("key-paste-plain", 0, 3, "書式なし貼付", KeyDef.Icon.emoji("🧹"),
new Action.Hotkey(List.of("CTRL", "SHIFT"), "V")),
new KeyDef("key-mute", 0, 4, "ミュート", KeyDef.Icon.emoji("🔇"),
new Action.Media("MUTE")),
new KeyDef("key-chrome", 1, 0, "Chrome", KeyDef.Icon.emoji("🌐"),
new Action.Launch("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", List.of())),
new KeyDef("key-vscode", 1, 1, "VS Code", KeyDef.Icon.emoji("💻"),
new Action.Launch("%LOCALAPPDATA%\\Programs\\Microsoft VS Code\\Code.exe", List.of())),
new KeyDef("key-packettracer", 1, 2, "Packet Tracer", KeyDef.Icon.emoji("🛜"),
new Action.Launch("C:\\Program Files\\Cisco Packet Tracer 8.2.1\\bin\\PacketTracer.exe", List.of())),
new KeyDef("key-notepad", 1, 3, "メモ帳", KeyDef.Icon.emoji("📝"),
new Action.Launch("notepad.exe", List.of())),
new KeyDef("key-terminal", 1, 4, "ターミナル", KeyDef.Icon.emoji("⬛"),
new Action.Launch("wt.exe", List.of())),
new KeyDef("key-prev", 2, 0, "前へ", KeyDef.Icon.emoji("⏮️"),
new Action.Media("PREV")),
new KeyDef("key-playpause", 2, 1, "再生/停止", KeyDef.Icon.emoji("⏯️"),
new Action.Media("PLAY_PAUSE")),
new KeyDef("key-next", 2, 2, "次へ", KeyDef.Icon.emoji("⏭️"),
new Action.Media("NEXT")),
new KeyDef("key-voldown", 2, 3, "音量-", KeyDef.Icon.emoji("🔉"),
new Action.Media("VOL_DOWN")),
new KeyDef("key-volup", 2, 4, "音量+", KeyDef.Icon.emoji("🔊"),
new Action.Media("VOL_UP"))
));

PageDef study = new PageDef("page-study", "勉強", 3, 5, List.of(
new KeyDef("key-pingt", 0, 0, "Ping-t", KeyDef.Icon.emoji("📚"),
new Action.Open("https://ping-t.com")),
new KeyDef("key-notebooklm", 0, 1, "NotebookLM", KeyDef.Icon.emoji("📓"),
new Action.Open("https://notebooklm.google.com")),
new KeyDef("key-calc", 0, 2, "電卓", KeyDef.Icon.emoji("🧮"),
new Action.Launch("calc.exe", List.of())),
new KeyDef("key-ipconfig", 0, 3, "IP確認", KeyDef.Icon.emoji("🔍"),
new Action.Launch("cmd.exe", List.of("/k", "ipconfig"))),
new KeyDef("key-downloads", 0, 4, "DLフォルダ", KeyDef.Icon.emoji("📥"),
new Action.Open("%USERPROFILE%\\Downloads")),
new KeyDef("key-ios-init", 1, 0, "初期設定", KeyDef.Icon.emoji("⚙️"),
new Action.Text("enable\nconfigure terminal\nno ip domain-lookup\nline console 0\nlogging synchronous\nexec-timeout 0 0\nend\n", null)),
new KeyDef("key-ios-save", 1, 1, "設定保存", KeyDef.Icon.emoji("💾"),
new Action.Text("write memory\n", null)),
new KeyDef("key-ios-ifbrief", 1, 2, "IF確認", KeyDef.Icon.emoji("🔎"),
new Action.Text("show ip interface brief\n", null)),
new KeyDef("key-ios-run", 1, 3, "設定確認", KeyDef.Icon.emoji("📜"),
new Action.Text("show running-config\n", null)),
new KeyDef("key-ios-route", 1, 4, "経路確認", KeyDef.Icon.emoji("🛣️"),
new Action.Text("show ip route\n", null)),
new KeyDef("key-ios-vlan", 2, 0, "VLAN確認", KeyDef.Icon.emoji("🔌"),
new Action.Text("show vlan brief\n", null)),
new KeyDef("key-ios-enable", 2, 1, "特権", KeyDef.Icon.emoji("🔑"),
new Action.Text("enable\n", null)),
new KeyDef("key-snap-left", 2, 2, "左に寄せる", KeyDef.Icon.emoji("⬅️"),
new Action.Hotkey(List.of("WIN"), "LEFT")),
new KeyDef("key-snap-right", 2, 3, "右に寄せる", KeyDef.Icon.emoji("➡️"),
new Action.Hotkey(List.of("WIN"), "RIGHT")),
new KeyDef("key-desktop", 2, 4, "デスクトップ", KeyDef.Icon.emoji("🖥️"),
new Action.Hotkey(List.of("WIN"), "D"))
));

PageDef phrases = new PageDef("page-phrases", "定型文", 3, 5, List.of(
new KeyDef("key-ph-otsukare", 0, 0, "お疲れ様", KeyDef.Icon.emoji("👋"),
new Action.Text("お疲れ様です。", null)),
new KeyDef("key-ph-osewa", 0, 1, "お世話に", KeyDef.Icon.emoji("🏢"),
new Action.Text("いつもお世話になっております。", null)),
new KeyDef("key-ph-thanks", 0, 2, "お礼", KeyDef.Icon.emoji("🙏"),
new Action.Text("ご対応いただきありがとうございます。", null)),
new KeyDef("key-ph-ryokai", 0, 3, "了解", KeyDef.Icon.emoji("👌"),
new Action.Text("了解しました。", null)),
new KeyDef("key-ph-kakunin", 0, 4, "確認中", KeyDef.Icon.emoji("⏳"),
new Action.Text("確認いたします。少々お待ちください。", null)),
new KeyDef("key-ph-workmemo", 1, 0, "作業メモ", KeyDef.Icon.emoji("📑"),
new Action.Text("\n\n\n\n", null)),
new KeyDef("key-ph-incident", 1, 1, "障害記録", KeyDef.Icon.emoji("🚨"),
new Action.Text("\n\n\n\n\n", null))
));

PageDef web = new PageDef("page-web", "Web", 3, 5, List.of(
                new KeyDef("key-web-youtube", 0, 0, "YouTube", KeyDef.Icon.emoji("📺"),
                        new Action.Open("https://www.youtube.com")),
                new KeyDef("key-web-hulu", 0, 1, "Hulu", KeyDef.Icon.emoji("🎬"),
                        new Action.Open("https://www.hulu.jp")),
                new KeyDef("key-web-x", 0, 2, "X", KeyDef.Icon.text("X"),
                        new Action.Open("https://x.com")),
                new KeyDef("key-web-amazon", 0, 3, "Amazon", KeyDef.Icon.emoji("🛒"),
                        new Action.Open("https://www.amazon.co.jp")),
                new KeyDef("key-web-aibou", 0, 4, "相棒", KeyDef.Icon.emoji("🐸"),
                        new Action.Open("https://muse.ai")),
                new KeyDef("key-web-udemy", 1, 0, "Udemy", KeyDef.Icon.emoji("🎓"),
                        new Action.Open("https://www.udemy.com")),
                new KeyDef("key-yt-back", 2, 0, "10秒戻す", KeyDef.Icon.emoji("⏪"),
                        new Action.Hotkey(List.of(), "J")),
                new KeyDef("key-yt-playpause", 2, 1, "再生/停止", KeyDef.Icon.emoji("⏯️"),
                        new Action.Hotkey(List.of(), "K")),
                new KeyDef("key-yt-fwd", 2, 2, "10秒進む", KeyDef.Icon.emoji("⏩"),
                        new Action.Hotkey(List.of(), "L")),
                new KeyDef("key-yt-next", 2, 3, "次へ", KeyDef.Icon.emoji("⏭️"),
                        new Action.Hotkey(List.of("SHIFT"), "N")),
                new KeyDef("key-yt-fullscreen", 2, 4, "全画面", KeyDef.Icon.emoji("⛶"),
                        new Action.Hotkey(List.of(), "F"))
        ));

        Profile profile = new Profile("prof-default", "標準", null, List.of(main, study, phrases, web));
return new Deck(1, List.of(profile));
}
}
