package mouse;

//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Cache
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

type Cache struct {
	name string;
	diag string;
	cache []*Phrase;
	last int;
}

func NewCache(name string, diag string) *Cache {
	c:=Cache{};
	c.name=name;
	c.diag=diag;
	return &c;
}

func (this *Cache) reset(cacheSize int) {
	this.cache = make([]*Phrase,cacheSize,cacheSize);
	this.last=0;
}

func (this *Cache) find(pos int,cacheSize int) *Phrase{
	if (cacheSize==0){ return nil; }
	for _,p:=range this.cache {
		if (p!=nil && p.start==pos){ return p; }
	}
	return nil;
}

func (this *Cache) save(p *Phrase,cacheSize int){
	if(cacheSize==0){ return }
	this.last=(this.last+1)%cacheSize;
	this.cache[this.last]=p;
}


