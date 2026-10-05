package tj.navortj.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int NAVY=0xff071b2c, TEAL=0xff15b8af, ORANGE=0xffff943d, BG=0xfff3f6fa, INK=0xff183345, MUTED=0xff687d8c;
    private final java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(3);
    private boolean syncing;
    private long lastSync;
    private ShopRepository store;
    private boolean tj;
    private String tab="home", query="", category="";
    private Product detail;
    private LinearLayout root, body, results, navigation;
    private ScrollView scroll;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        try { store=new ShopRepository(this); } catch(Exception e) {
            TextView error=text("NAVORTJ\nКаталогро хондан нашуд / Не удалось прочитать каталог.\nproducts.json / shop.json",18,INK,true);
            error.setPadding(dp(24),dp(80),dp(24),dp(24)); setContentView(error); return;
        }
        tj=store.tajik();
        if(saved!=null) { tab=saved.getString("tab","home");query=saved.getString("query","");category=saved.getString("category","");detail=store.find(saved.getString("detail","")); }
        render();
    }
    @Override protected void onResume(){super.onResume();if(store!=null)refreshCatalog(false);}
    @Override protected void onDestroy(){workers.shutdownNow();super.onDestroy();}
    private void refreshCatalog(boolean force){
        if(syncing||(!force&&System.currentTimeMillis()-lastSync<30000))return;
        syncing=true;lastSync=System.currentTimeMillis();
        workers.execute(()->{try{org.json.JSONArray data=CatalogSync.catalog();ShopRepository.decode(data);
            runOnUiThread(()->{syncing=false;if(isDestroyed())return;try{String id=detail==null?null:detail.id;store.replaceCatalog(data);if(id!=null)detail=store.find(id);if(!categories().contains(category))category="";render();if(force)toast(tr("Каталог нав шуд","Каталог обновлён"));}catch(Exception e){if(force)toast(tr("Каталог нав нашуд","Не удалось обновить каталог"));}});
        }catch(Exception e){runOnUiThread(()->{syncing=false;if(force&&!isDestroyed())toast(tr("Интернетро санҷед. Каталоги охирин нигоҳ дошта шуд.","Проверьте интернет. Последний каталог сохранён."));});}});
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("tab",tab);out.putString("query",query);out.putString("category",category);out.putString("detail",detail==null?"":detail.id);
    }
    private String tr(String a,String b){return tj?a:b;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void space(LinearLayout parent,int size){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    private void weighted(LinearLayout row,View v){row.addView(v,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));}
    private Button button(String label,int color,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(color==BG?INK:Color.WHITE);b.setBackground(bg(color,12));b.setMinHeight(dp(44));b.setMinimumHeight(dp(44));b.setPadding(dp(12),dp(5),dp(12),dp(5));b.setOnClickListener(v->action.run());return b;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private String money(double n){return String.format(Locale.US,n==Math.floor(n)?"%.0f сомонӣ":"%.2f сомонӣ",n);}
    private String price(Product p){return p.price>0?money(p.price):tr("Нархро пурсед","Уточните цену");}
    private void go(String destination){detail=null;tab=destination;query="";category="";render();}
    private void render(){
        root=column();root.setBackgroundColor(BG);
        // Insets keep all controls visible with Android 15 edge-to-edge enforcement.
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();
        LinearLayout header=row();header.setPadding(dp(18),dp(12),dp(14),dp(12));header.setBackgroundColor(NAVY);
        LinearLayout brand=column();brand.addView(text("NAVORTJ",23,Color.WHITE,true));brand.addView(text(tr("Технология барои шумо","Технологии для вас"),11,0xffa8c4d3,false));
        brand.setOnClickListener(v->go("home"));weighted(header,brand);
        Button lang=button(tj?"RU":"TJ",TEAL,()->{tj=!tj;store.prefs.edit().putBoolean("tajik",tj).apply();category="";render();});header.addView(lang,new LinearLayout.LayoutParams(dp(58),dp(42)));root.addView(header);
        scroll=new ScrollView(this);scroll.setFillViewport(true);body=column();body.setPadding(dp(16),dp(16),dp(16),dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(detail!=null)showDetail();else switch(tab){case "catalog":showCatalog(false);break;case "favorites":showCatalog(true);break;case "cart":showCart();break;case "settings":showSettings();break;default:showHome();}
        navigation=row();navigation.setBackgroundColor(Color.WHITE);navigation.setPadding(dp(3),dp(8),dp(3),dp(8));
        nav("home",tr("Асосӣ","Главная"));nav("catalog",tr("Каталог","Каталог"));nav("favorites",tr("Дилхоҳ","Избранное"));nav("cart",tr("Сабад","Корзина")+" ("+store.count()+")");nav("settings",tr("Танзим","Ещё"));root.addView(navigation);
    }
    private void nav(String id,String label){TextView v=text(label,11,tab.equals(id)&&detail==null?TEAL:MUTED,true);v.setGravity(Gravity.CENTER);v.setPadding(0,dp(12),0,dp(12));v.setMinHeight(dp(48));v.setOnClickListener(w->go(id));weighted(navigation,v);}
    private void title(String s){body.addView(text(s,25,INK,true));space(body,12);}
    private void showHome(){
        LinearLayout hero=column();hero.setPadding(dp(22),dp(24),dp(22),dp(24));hero.setBackground(bg(NAVY,22));
        hero.addView(text(tr("ХУШ ОМАДЕД БА NAVORTJ","ДОБРО ПОЖАЛОВАТЬ В NAVORTJ"),11,TEAL,true));space(hero,12);
        hero.addView(text(tr("Интихоби осон.\nФармоиши қулай.","Легко выбрать.\nУдобно заказать."),29,Color.WHITE,true));space(hero,14);
        hero.addView(text(tr("Энергия • Амният • Электроника","Энергия • Безопасность • Электроника"),14,0xffc3d8e3,false));space(hero,18);
        hero.addView(button(tr("Каталогро кушоед →","Открыть каталог →"),ORANGE,()->go("catalog")));body.addView(hero);space(body,20);
        body.addView(text(tr("Категорияҳо","Категории"),21,INK,true));space(body,10);
        for(String cat:categories()){Button b=button(cat+"  →",BG,()->{tab="catalog";category=cat;query="";render();});body.addView(b);space(body,5);}
        space(body,18);body.addView(text(tr("Барои шумо","Для вас"),21,INK,true));space(body,10);
        for(int i=0;i<Math.min(3,store.products.size());i++)productCard(body,store.products.get(i));
        body.addView(button(tr("Нав кардани каталог","Обновить каталог"),TEAL,()->refreshCatalog(true)));
    }
    private LinkedHashSet<String> categories(){LinkedHashSet<String> s=new LinkedHashSet<>();for(Product p:store.products)s.add(p.category(tj));return s;}
    private void showCatalog(boolean onlyFavorites){
        title(onlyFavorites?tr("Молҳои дилхоҳ","Избранное"):tr("Каталог","Каталог"));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(16);search.setHint(tr("Ҷустуҷӯи мол…","Поиск товаров…"));search.setPadding(dp(14),dp(10),dp(14),dp(10));search.setBackground(bg(Color.WHITE,14));search.setText(query);body.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));space(body,12);
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();
        ArrayList<String> cats=new ArrayList<>();cats.add("");cats.addAll(categories());
        for(String cat:cats){Button b=button(cat.isEmpty()?tr("Ҳама","Все"):cat,category.equals(cat)?TEAL:NAVY,()->{category=cat;query=search.getText().toString();render();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));lp.setMargins(0,0,dp(7),0);chips.addView(b,lp);}filters.addView(chips);body.addView(filters);space(body,12);
        results=column();body.addView(results);fillResults(onlyFavorites);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();fillResults(onlyFavorites);}public void afterTextChanged(Editable s){}});
    }
    private void fillResults(boolean onlyFavorites){results.removeAllViews();String q=query.toLowerCase(Locale.ROOT).trim();int count=0;
        for(Product p:store.products){if(onlyFavorites&&!store.favorites.contains(p.id))continue;if(!category.isEmpty()&&!category.equals(p.category(tj)))continue;
            if(!(p.nameTj+" "+p.nameRu+" "+p.categoryTj+" "+p.categoryRu+" "+p.subtitle(tj)).toLowerCase(Locale.ROOT).contains(q))continue;
            productCard(results,p);count++;
        }
        if(count==0)empty(results,onlyFavorites?tr("Ҳоло молҳои дилхоҳ нестанд","Пока нет избранных товаров"):tr("Мол ёфт нашуд","Товары не найдены"));
    }
    private View illustration(Product p,int height){
        if(CatalogSync.remoteImage(p.image)){
            ImageView img=new ImageView(this);img.setBackground(bg(0xffe8f7f5,16));img.setScaleType(ImageView.ScaleType.FIT_CENTER);img.setContentDescription(p.name(tj));img.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));
            workers.execute(()->{try{android.graphics.Bitmap bitmap=CatalogSync.image(getApplicationContext(),p.image);runOnUiThread(()->{if(!isDestroyed())img.setImageBitmap(bitmap);});}catch(Exception ignored){}});return img;
        }
        if(!p.image.isEmpty()&&!p.image.contains("..")&&!p.image.startsWith("/"))try(InputStream in=getAssets().open("images/"+p.image)){ImageView img=new ImageView(this);img.setImageDrawable(android.graphics.drawable.Drawable.createFromStream(in,p.image));img.setScaleType(ImageView.ScaleType.FIT_CENTER);img.setAdjustViewBounds(true);img.setContentDescription(p.name(tj));img.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));return img;}catch(Exception ignored){}
        TextView v=text(symbol(p.kind),height>100?64:32,TEAL,true);v.setGravity(Gravity.CENTER);v.setBackground(bg(0xffe8f7f5,16));v.setContentDescription(p.name(tj));v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));return v;
    }
    private String symbol(String kind){switch(kind){case "solar":return "☀";case "battery":return "▣";case "camera":return "◉";case "router":return "⌁";case "tv":return "▤";default:return "ϟ";}}
    private void productCard(LinearLayout parent,Product p){
        LinearLayout card=column();card.setBackground(bg(Color.WHITE,18));card.setPadding(dp(14),dp(14),dp(14),dp(14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(12));parent.addView(card,cp);
        LinearLayout top=row();View art=illustration(p,72);top.addView(art,new LinearLayout.LayoutParams(dp(80),dp(72)));LinearLayout info=column();info.setPadding(dp(12),0,0,0);info.addView(text(p.name(tj),17,INK,true));space(info,4);info.addView(text(p.subtitle(tj),12,MUTED,false));space(info,6);info.addView(text(price(p),14,TEAL,true));weighted(top,info);top.setOnClickListener(v->{detail=p;render();});card.addView(top);space(card,12);
        LinearLayout actions=row();weighted(actions,button(tr("Ба сабад","В корзину"),NAVY,()->{store.quantity(p.id,store.quantityOf(p.id)+1);refreshNav();toast(tr("Ба сабад илова шуд","Добавлено в корзину"));}));
        View gap=new View(this);actions.addView(gap,new LinearLayout.LayoutParams(dp(8),1));Button fav=button(store.favorites.contains(p.id)?"♥":"♡",store.favorites.contains(p.id)?ORANGE:TEAL,()->{store.toggle(p.id);render();});fav.setContentDescription(tr("Молҳои дилхоҳ","Избранное"));actions.addView(fav,new LinearLayout.LayoutParams(dp(52),dp(44)));card.addView(actions);
    }
    private void refreshNav(){if(navigation!=null){navigation.removeAllViews();nav("home",tr("Асосӣ","Главная"));nav("catalog",tr("Каталог","Каталог"));nav("favorites",tr("Дилхоҳ","Избранное"));nav("cart",tr("Сабад","Корзина")+" ("+store.count()+")");nav("settings",tr("Танзим","Ещё"));}}
    private void showDetail(){Product p=detail;
        body.addView(button(tr("← Бозгашт","← Назад"),TEAL,()->{detail=null;render();}));space(body,14);body.addView(illustration(p,200));space(body,18);title(p.name(tj));body.addView(text(price(p),22,TEAL,true));space(body,12);body.addView(text(p.description(tj),16,INK,false));space(body,18);
        body.addView(button(tr("Ба сабад илова кунед","Добавить в корзину"),ORANGE,()->{store.quantity(p.id,store.quantityOf(p.id)+1);refreshNav();toast(tr("Ба сабад илова шуд","Добавлено в корзину"));}));space(body,10);
        body.addView(button(store.favorites.contains(p.id)?tr("♥ Аз дилхоҳ бароред","♥ Убрать из избранного"):tr("♡ Ба дилхоҳ илова кунед","♡ Добавить в избранное"),TEAL,()->{store.toggle(p.id);render();}));space(body,10);
        body.addView(button(tr("Мубодила","Поделиться"),NAVY,()->share(p.name(tj)+"\n"+price(p)+"\n"+p.description(tj)+"\nNAVORTJ")));space(body,10);
        body.addView(button("WhatsApp",TEAL,()->whatsapp(tr("Салом! Дар бораи ин мол маълумот мехоҳам: ","Здравствуйте! Хочу узнать о товаре: ")+p.name(tj))));
    }
    private void empty(LinearLayout target,String s){TextView v=text(s,17,MUTED,false);v.setGravity(Gravity.CENTER);v.setPadding(dp(12),dp(48),dp(12),dp(48));target.addView(v);}
    private void showCart(){title(tr("Сабади харид","Корзина"));
        if(store.cart.isEmpty()){empty(body,tr("Сабад холӣ аст","Корзина пуста"));body.addView(button(tr("Ба каталог","В каталог"),TEAL,()->go("catalog")));return;}
        for(Map.Entry<String,Integer> e:new ArrayList<>(store.cart.entrySet())){Product p=store.find(e.getKey());if(p==null)continue;int q=e.getValue();
            LinearLayout card=column();card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(bg(Color.WHITE,16));card.addView(text(p.name(tj),18,INK,true));space(card,5);card.addView(text(price(p),15,TEAL,true));space(card,12);
            LinearLayout controls=row();controls.addView(button("−",NAVY,()->{store.quantity(p.id,q-1);render();}),new LinearLayout.LayoutParams(dp(48),dp(44)));
            TextView qty=text(String.valueOf(q),18,INK,true);qty.setGravity(Gravity.CENTER);controls.addView(qty,new LinearLayout.LayoutParams(dp(48),-2));controls.addView(button("+",NAVY,()->{store.quantity(p.id,q+1);render();}),new LinearLayout.LayoutParams(dp(48),dp(44)));View gap=new View(this);controls.addView(gap,new LinearLayout.LayoutParams(0,1,1));controls.addView(button("×",ORANGE,()->{store.quantity(p.id,0);render();}),new LinearLayout.LayoutParams(dp(48),dp(44)));card.addView(controls);body.addView(card);space(body,10);
        }
        body.addView(text(tr("Миқдор: ","Количество: ")+store.count(),16,INK,true));space(body,8);
        body.addView(text((store.hasUnpriced()?tr("Ҷамъ барои молҳои нархдор: ","Сумма товаров с ценой: "):tr("Ҷамъ: ","Итого: "))+money(store.total()),20,INK,true));
        if(store.hasUnpriced()){space(body,8);body.addView(text(tr("Нарх ва маблағи ниҳоиро фурӯшанда тасдиқ мекунад.","Цены и окончательную сумму подтвердит продавец."),14,MUTED,false));}
        space(body,18);body.addView(button(tr("Фармоиш дар WhatsApp","Заказать в WhatsApp"),TEAL,this::checkout));space(body,10);
        body.addView(button(tr("Холӣ кардани сабад","Очистить корзину"),NAVY,()->new AlertDialog.Builder(this).setMessage(tr("Сабадро холӣ мекунед?","Очистить корзину?")).setPositiveButton(tr("Бале","Да"),(d,w)->{store.cart.clear();store.save();render();}).setNegativeButton(tr("Не","Нет"),null).show()));
    }
    private String orderText(){StringBuilder b=new StringBuilder(tr("Салом! Фармоиш аз NAVORTJ:\n","Здравствуйте! Заказ из NAVORTJ:\n"));int i=1;
        for(Map.Entry<String,Integer> e:store.cart.entrySet()){Product p=store.find(e.getKey());if(p==null)continue;b.append(i++).append(". ").append(p.name(tj)).append(" × ").append(e.getValue()).append(" — ").append(p.price>0?money(p.price*e.getValue()):tr("Нархро пурсед","Уточните цену")).append('\n');}
        b.append(tr("Ҷамъ барои молҳои нархдор: ","Сумма товаров с ценой: ")).append(money(store.total())).append('\n');if(store.hasUnpriced())b.append(tr("Маблағи ниҳоиро тасдиқ кунед.\n","Подтвердите окончательную сумму.\n"));return b.toString();
    }
    private void checkout(){if(store.cart.isEmpty())return;
        LinearLayout form=column();form.setPadding(dp(24),dp(12),dp(24),dp(8));EditText name=new EditText(this);name.setHint(tr("Номи шумо (ихтиёрӣ)","Ваше имя (необязательно)"));form.addView(name);EditText address=new EditText(this);address.setHint(tr("Суроға / эзоҳ (ихтиёрӣ)","Адрес / комментарий (необязательно)"));form.addView(address);
        new AlertDialog.Builder(this).setTitle(tr("Фармоиш","Заказ")).setView(form).setPositiveButton(tr("Давом","Продолжить"),(d,w)->{String msg=orderText();if(!name.getText().toString().trim().isEmpty())msg+=tr("Ном: ","Имя: ")+name.getText()+"\n";if(!address.getText().toString().trim().isEmpty())msg+=tr("Эзоҳ: ","Комментарий: ")+address.getText();whatsapp(msg);}).setNegativeButton(tr("Бекор","Отмена"),null).show();
    }
    private void whatsapp(String message){String phone=store.phone().replaceAll("[^0-9]","");if(!phone.matches("[1-9][0-9]{6,14}")){new AlertDialog.Builder(this).setMessage(tr("Аввал дар Танзим рақами WhatsApp-и мағозаро гузоред.","Сначала укажите WhatsApp магазина в настройках.")).setPositiveButton(tr("Танзим","Настройки"),(d,w)->go("settings")).setNegativeButton(tr("Бекор","Отмена"),null).show();return;}
        open("https://wa.me/"+phone+"?text="+Uri.encode(message));
    }
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){toast(tr("Барнома барои кушодани пайванд ёфт нашуд","Не найдено приложение для открытия ссылки"));}}
    private void share(String content){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,content);try{startActivity(Intent.createChooser(i,tr("Мубодила","Поделиться")));}catch(ActivityNotFoundException e){toast(tr("Барнома ёфт нашуд","Приложение не найдено"));}}
    private void showSettings(){title(tr("Танзим ва тамос","Настройки и контакты"));body.addView(text(tr("Рақами WhatsApp-и мағоза","WhatsApp магазина"),17,INK,true));space(body,8);
        EditText phone=new EditText(this);phone.setSingleLine(true);phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);phone.setHint("+992 …");phone.setText(store.phone());body.addView(phone);space(body,10);
        body.addView(button(tr("Рақамро нигоҳ доред","Сохранить номер"),TEAL,()->{String n=phone.getText().toString().replaceAll("[^0-9]","");if(!n.matches("[1-9][0-9]{6,14}")){toast(tr("Рақами дурустро бо коди кишвар гузоред","Введите правильный номер с кодом страны"));return;}store.prefs.edit().putString("whatsapp",n).apply();toast(tr("Нигоҳ дошта шуд","Сохранено"));}));space(body,8);
        body.addView(text(tr("Ин танзим танҳо дар ҳамин телефон нигоҳ дошта мешавад. Барои ҳамаи муштариён рақамро соҳиби мағоза танзим мекунад.","Эта настройка сохраняется только на этом телефоне. Общий номер для покупателей задаёт владелец магазина."),13,MUTED,false));space(body,20);
        body.addView(button(tr("Забон: Тоҷикӣ → Русский","Язык: Русский → Тоҷикӣ"),NAVY,()->{tj=!tj;store.prefs.edit().putBoolean("tajik",tj).apply();category="";render();}));space(body,20);
        String email=store.config("email");if(!email.isEmpty()){body.addView(text("Email: "+email,15,INK,false));space(body,10);}
        String addr=store.config(tj?"address_tj":"address_ru");if(!addr.isEmpty()){body.addView(text(addr,17,INK,true));space(body,10);}
        body.addView(text(store.config(tj?"delivery_tj":"delivery_ru"),15,MUTED,false));space(body,10);
        String instagram=store.config("instagram");if(instagram.matches("[A-Za-z0-9_.]+")){body.addView(button("Instagram",TEAL,()->open("https://www.instagram.com/"+instagram+"/")));space(body,10);}
        body.addView(button(tr("Ба мағоза нависед","Написать в магазин"),TEAL,()->whatsapp(tr("Салом! Аз NAVORTJ менависам.","Здравствуйте! Пишу из NAVORTJ."))));space(body,20);
        body.addView(button(tr("Панели админ","Панель администратора"),NAVY,()->open(CatalogSync.ORIGIN)));space(body,10);body.addView(button(tr("Нав кардани каталог","Обновить каталог"),TEAL,()->refreshCatalog(true)));space(body,20);body.addView(text("NAVORTJ 1.1.0",17,INK,true));space(body,8);body.addView(text(tr("Сабад ва молҳои дилхоҳ дар телефон нигоҳ дошта мешаванд. Фармоишро худатон дар WhatsApp мефиристед. Дар ин барнома пардохти онлайн ва ҳисоби корбар нест.","Корзина и избранное хранятся на телефоне. Заказ вы отправляете самостоятельно в WhatsApp. Онлайн-оплаты и регистрации в этой версии нет."),14,MUTED,false));
    }
    @Override public void onBackPressed(){if(detail!=null){detail=null;render();}else if(!tab.equals("home")){go("home");}else super.onBackPressed();}
}

