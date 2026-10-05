package tj.navortj.app;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
final class CatalogSync {
    static final String ORIGIN="https://navor-tj-admin.saidolimov280.chatgpt.site";
    static byte[] download(String url,int limit) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);
        try {if(c.getResponseCode()!=200)throw new IOException("Unavailable");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Too large");out.write(b,0,n);}return out.toByteArray();
            }
        }finally{c.disconnect();}
    }
    static JSONArray catalog() throws Exception {return new JSONArray(new String(download(ORIGIN+"/api/catalog",10*1024*1024),StandardCharsets.UTF_8));}
    static boolean remoteImage(String url){return url.startsWith(ORIGIN+"/api/images/")&&url.substring((ORIGIN+"/api/images/").length()).matches("[a-f0-9-]+\\.(jpg|png|webp)");}
    static Bitmap image(Context context,String url) throws Exception {
        if(!remoteImage(url))throw new IOException("Invalid image");
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));StringBuilder key=new StringBuilder();for(byte v:hash)key.append(String.format("%02x",v&255));
        File file=new File(context.getCacheDir(),"navor-"+key);
        byte[] data;
        if(file.isFile()){try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);data=out.toByteArray();}}
        else{data=download(url,8*1024*1024);try(FileOutputStream out=new FileOutputStream(file)){out.write(data);}}
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);
        if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Invalid image");
        options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1600||options.outHeight/options.inSampleSize>1600)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(data,0,data.length,options);
    }
}
