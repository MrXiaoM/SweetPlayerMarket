package top.mrxiaom.sweet.playermarket.database;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringJoiner;

public class SQLiteLibSimple {
    private final File dictFile;
    private final String loadExtension, jiebaDict;
    private SQLiteLibSimple(File libFile, File dictFile) {
        this.dictFile = dictFile;
        this.loadExtension = func("load_extension", libFile.getAbsolutePath() , "sqlite3_simple_init");
        this.jiebaDict = func("jieba_dict", dictFile.getAbsolutePath());
    }

    public void apply(Statement stat) throws SQLException {
        stat.execute(loadExtension);
        if (dictFile.exists()) {
            stat.execute(jiebaDict);
        }
    }

    private static String func(String funcName, String... args) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String arg : args) {
            joiner.add("'" + arg + "'");
        }
        return "SELECT " + funcName + "(" + joiner + ");";
    }

    public static SQLiteLibSimple init(File sqliteFolder, Statement stat) throws SQLException {
        File libFile;
        if (new File(sqliteFolder, "simple.dll").exists()) {
            // Windows 比较特殊一点，动态链接库没有 lib 前缀
            libFile = new File(sqliteFolder, "simple");
            stat.execute(func("load_extension", libFile.getAbsolutePath() , "sqlite3_simple_init"));
        } else {
            // Linux/MacOS
            libFile = new File(sqliteFolder, "libsimple");
            stat.execute(func("load_extension", libFile.getAbsolutePath() , "sqlite3_simple_init"));
        }
        // 加载词典
        File dictFile = new File(sqliteFolder, "dict");
        if (dictFile.exists()) {
            stat.execute(func("jieba_dict", dictFile.getAbsolutePath()));
        }
        return new SQLiteLibSimple(libFile, dictFile);
    }
}
