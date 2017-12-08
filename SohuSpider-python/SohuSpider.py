#coding:utf-8
import sys,re,os
from selenium import webdriver
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
import json

reload(sys)
sys.setdefaultencoding('utf-8')

def save(filename, contents, path = './'): # 保存内容入本文
	filename = path + filename
	fh = open(filename,'w')
	fh.write(contents)
	fh.close();

def getPage(url):
	try:
		driver.get(url)
		return driver
	except:
		print "can not get ",url

def savePage(content,url):
	content = content.page_source
	newsid = re.findall(r"\d+_\d+",url)[0]
	filename = newsid+"_cmt.txt"
	save(filename,content)

def ChromeInitial():
	options=webdriver.ChromeOptions()
	prefs={
		'profile.default_content_setting_values': {
			'images': 2
		}
	} #设置不加载图片
	options.add_experimental_option('prefs',prefs)
	driver = webdriver.Chrome(chrome_options=options)
	js="document.documentElement.scrollTop=10000" #chrome用这个
	return  driver, js # chrome 浏览器，用于可视化debug

def PhantomJSInitial():
	dcap = dict(DesiredCapabilities.PHANTOMJS)
	dcap["phantomjs.page.settings.userAgent"] = ("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.221 Safari/537.36 SE 2.X MetaSr 1.0") #设置user-agent请求头
	dcap["phantomjs.page.settings.loadImages"] = False #禁止加载图片
	 
	driver = webdriver.PhantomJS(executable_path=r'/Users/apple/phantomjs-2.1.1-macosx/bin/phantomjs',desired_capabilities=dcap) 
	driver.set_page_load_timeout(20) #设置页面最长加载时间为20s
	js="document.body.scrollTop=10000" #phantomjs 用这个，玄学。
	return driver, js # phantomjs非可视化浏览器，用于提高爬取速度

def extract(content, js, newsid, url):
	try:
		title =content.find_element_by_class_name('title-info').text
		time = content.find_element_by_class_name('time').text
		dispcont = content.find_element_by_class_name('display-content').text
		cmtnum = content.find_element_by_class_name('c-comments-num').text
		author =content.find_element_by_class_name('name').text
		newstype = content.find_element_by_class_name('back-sub').text

		if cmtnum != '':
			cmturl = 'http://m.sohu.com/cm/' + url.split('/')[-1]
			cmts = getcmts(cmturl,js)
		else:
			print "no comments"
			cmtnum = "0"
			cmts = ''
		newsdata ={'newsid':newsid, 'newsurl':url, 'author':author, 'title':title, 'type':newstype,'time':time, 'content':dispcont, 'cmtnum':cmtnum, 'cmts':cmts}
		newsjson = json.dumps(newsdata, encoding='utf-8', ensure_ascii=False)
		filename = newsid + ".json"
		save(filename,newsjson, './data/')
	except :
		print "Can not find wanted content"
		pass

def getcmts(url,js):
	content = getPage(url)
	content.implicitly_wait(3)#设置超时时间为1s
	for i in range(1,100): #设置查找
		content.execute_script(js)
		try:
			getmore = content.find_element_by_class_name('c-getmore')
			if getmore.text == "没有更多啦!" :
				break
		except:
			pass
	content.implicitly_wait(4) # 将超时时间改回10秒

	cmt_all = []
	try:
		cmtitems = content.find_elements_by_class_name('c-comment-item')
		for item in cmtitems:
			cmter = item.find_element_by_class_name('c-username').text
			cmt = item.find_elements_by_class_name('c-discuss')[-1].text #这里只保留最后一个，因为嵌套评论中该用户的评论在最后
			cmttime = item.find_element_by_class_name('c-item-date').text
			comment = {'cmter':cmter, 'cmt': cmt, 'cmttime':cmttime}
			cmt_all.append(comment)
		return cmt_all
	except:
		print "can not find comment"
		return ""	

if __name__ == '__main__':
	
	# driver, js = ChromeInitial()
	driver, js = PhantomJSInitial()

	i = 0
	with open('./data/aurl.txt') as f:
		for url in f:
			url = url[:-1]

			if not 'https' in url:
				continue 
			newsid = re.findall(r"\d+_\d+",url)[0]
			if os.path.exists('./data/'+ newsid+ '.json') == True: # 如果用户已经被抓取过，跳过
				print "webpage", newsid ,"already exists"
				continue
			content = getPage(url)
			extract(content, js, newsid, url)
			i += 1; print i,"page done"

	driver.quit()










