package com.jerry.schedule108;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 108班課表的單一資料來源。
 * entries 的排序規則：先依「星期」排序，同一天內再依「節次時間」排序
 * （星期一第一節、星期一第二節...星期一最後一節、星期二第一節...）。
 * 這個順序非常重要：ClassStatusService 跟 NextClassWidgetProvider
 * 都用「掃描陣列、找第一筆符合條件的」方式算下一節，陣列順序錯了
 * 下一節就會算錯（舊版的 bug 正是陣列排序寫反，变成「先排完所有星期的
 * 第一節，再排所有星期的第二節...」，導致常常誤判成隔一天的第一節）。
 */
class ScheduleData {

    static class Period {
        final String label;
        final int startMin, endMin;
        Period(String label, int sh, int sm, int eh, int em) {
            this.label = label;
            this.startMin = sh * 60 + sm;
            this.endMin = eh * 60 + em;
        }
        String toClock() {
            return String.format(java.util.Locale.TAIWAN, "%02d:%02d", startMin / 60, startMin % 60);
        }
    }

    static class Entry {
        final int weekday; // 1=一 ... 5=五
        final Period period;
        final String subject;
        final String teacher;
        Entry(int weekday, Period period, String subject, String teacher) {
            this.weekday = weekday;
            this.period = period;
            this.subject = subject;
            this.teacher = teacher;
        }
    }

    static class Status {
        final Entry current;
        final Entry next;
        Status(Entry current, Entry next) {
            this.current = current;
            this.next = next;
        }
    }

    static final Period P1 = new Period("第一節", 8, 10, 9, 0);
    static final Period P2 = new Period("第二節", 9, 10, 10, 0);
    static final Period P3 = new Period("第三節", 10, 10, 11, 0);
    static final Period P4 = new Period("第四節", 11, 10, 12, 0);
    static final Period P5 = new Period("第五節", 13, 0, 13, 50);
    static final Period P6 = new Period("第六節", 14, 0, 14, 50);
    static final Period P7 = new Period("第七節", 15, 5, 15, 55);
    static final Period P8 = new Period("第八節", 16, 0, 16, 50);

    private static String subjectOf(int wd, Period p) { return lookup(wd, p, true); }
    private static String teacherOf(int wd, Period p) { return lookup(wd, p, false); }

    private static String lookup(int wd, Period p, boolean wantSubject) {
        String[][] row;
        if (p == P1) row = new String[][]{null,
                {"英語文", "林紹恩"}, {"地理", "楊森豪"}, {"國文", "袁銘德"}, {"先備課程", "袁銘德"}, {"美術", "溫喬文"}};
        else if (p == P2) row = new String[][]{null,
                {"國文", "袁銘德"}, {"英語文", "林紹恩"}, {"國文", "袁銘德"}, {"彈性充補", "劉育瑄"}, {"歷史", "張欣儀"}};
        else if (p == P3) row = new String[][]{null,
                {"多元選修", "葉鑫驊"}, {"地球科學", "郭佳甄"}, {"地球科學", "郭佳甄"}, {"數學", "黃美玲"}, {"團體活動", ""}};
        else if (p == P4) row = new String[][]{null,
                {"多元選修", "葉鑫驊"}, {"化學", "林士倫"}, {"化學", "林士倫"}, {"國文", "袁銘德"}, {"團體活動", ""}};
        else if (p == P5) row = new String[][]{null,
                {"美術", "溫喬文"}, {"體育", "曾千威"}, {"行腳桃花源", "江佳蓓 李昆謙"}, {"地理", "楊森豪"}, {"班級活動", "袁銘德"}};
        else if (p == P6) row = new String[][]{null,
                {"數學", "黃美玲"}, {"生命教育", "徐雨堤"}, {"行腳桃花源", "江佳蓓 李昆謙"}, {"歷史", "張欣儀"}, {"數學", "黃美玲"}};
        else if (p == P7) row = new String[][]{null,
                {"健康與護理", "李蕙君"}, {"數學", "黃美玲"}, {"英語文", "林紹恩"}, {"英語文", "林紹恩"}, {"體育", "曾千威"}};
        else if (p == P8) row = new String[][]{null,
                {"讀書輔導", "徐雨堤"}, {"英文輔導課", "林紹恩"}, {"數學輔導課", "黃美玲"}, {"國文輔導課", "袁銘德"}, null};
        else return null;

        String[] cell = row[wd];
        if (cell == null) return null;
        return wantSubject ? cell[0] : cell[1];
    }

    static List<Entry> buildEntries() {
        List<Entry> list = new ArrayList<>();
        Period[] periods = {P1, P2, P3, P4, P5, P6, P7, P8};
        for (int wd = 1; wd <= 5; wd++) {
            for (Period p : periods) {
                String subject = subjectOf(wd, p);
                if (subject == null) continue;
                String teacher = teacherOf(wd, p);
                list.add(new Entry(wd, p, subject, teacher == null ? "" : teacher));
            }
        }
        return list;
    }

    static Status computeStatus(List<Entry> entries, Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun ... 6=Sat
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        Entry current = null;
        if (dow >= 1 && dow <= 5) {
            for (Entry e : entries) {
                if (e.weekday == dow && nowMin >= e.period.startMin && nowMin < e.period.endMin) {
                    current = e;
                    break;
                }
            }
        }

        Entry next = null;
        for (Entry e : entries) {
            if (e.weekday > dow || (e.weekday == dow && e.period.startMin > nowMin)) {
                next = e;
                break;
            }
        }
        if (next == null && !entries.isEmpty()) next = entries.get(0);

        return new Status(current, next);
    }
}
