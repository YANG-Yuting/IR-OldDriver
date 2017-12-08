# with open("1.txt",'w+') as fd:  
# 	fd.write('123')  
# with open("1.txt",'r+') as fd:   
# 	#fd.write('456')  
# 	print fd.read()
with open("1.txt",'a+') as fd:  
	print fd.read()
	fd.write('789')  
	print fd.read()