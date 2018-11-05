package trader.tool;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import trader.TraderMain;
import trader.common.exchangeable.Exchange;
import trader.common.exchangeable.MarketDayUtil;
import trader.common.util.ConversionUtil;
import trader.common.util.FileUtil;
import trader.common.util.TraderHomeUtil;

public class ServiceAction implements CmdAction, ApplicationListener<ContextClosedEvent> {

    File pidFile;

    @Override
    public String getCommand() {
        return "service";
    }

    @Override
    public void usage(PrintWriter writer) {
        writer.println("service");
        writer.println("\t启动交易服务");
    }

    @Override
    public int execute(PrintWriter writer, List<String> options) throws Exception {
        init();
        if ( !MarketDayUtil.isMarketDay(Exchange.SHFE, LocalDate.now()) ) {
            writer.println("非交易日: "+LocalDate.now());
            return 1;
        }
        long traderPid = getTraderPid();
        if ( traderPid>0 ) {
            writer.println("Trader进程已运行: "+traderPid);
            return 1;
        }
        writer.println("Starting trader from home " + TraderHomeUtil.getTraderHome());
        ConfigurableApplicationContext context = SpringApplication.run(TraderMain.class, options.toArray(new String[options.size()]));
        savePid();
        context.addApplicationListener(this);
        synchronized(this) {
            wait();
        }
        return 0;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        synchronized(this) {
            notify();
        }
    }

    private void init() {
        File workDir = TraderHomeUtil.getDirectory(TraderHomeUtil.DIR_WORK);
        workDir.mkdirs();
        pidFile = new File(workDir, "trader.pid");
    }

    /**
     * 判断pid文件所记载的trader进程是否存在
     */
    private long getTraderPid() throws Exception {
        long result = 0;
        if ( pidFile.exists() && pidFile.length()>0 ) {
            long pid = ConversionUtil.toLong( FileUtil.read(pidFile) );
            Optional<ProcessHandle> process = ProcessHandle.of(pid);
            if ( process.isPresent() ) {
                result = pid;
            }
        }
        return result;
    }

    /**
     * 保存pid到trader.pid文件
     */
    private void savePid() {
        try{
            FileUtil.save(pidFile, ""+ProcessHandle.current().pid());
        }catch(Throwable t) {}
    }

}
