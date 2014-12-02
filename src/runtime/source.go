package mouse

import "unicode/utf8"
import "io/ioutil"

type Source interface {
	End() int;
	At(i int, j int) string;
	RuneAt(i int) rune;
	Where(p int) string;
}

type StringSource struct {
	source string;
}

func NewStringSource(s string) *StringSource {
	ss:=StringSource{s};
	return &ss;
}

func (s StringSource) End() int {
	return utf8.RuneCountInString(s.source);
}

func (s StringSource) At(i int, j int) string {
	//FIXME this is horrible
	out:=make([]rune,0,j-i);
	for si,r:=range s.source {
		if(si>=i && si<j){
			out=append(out,r);
		}
	}
	return string(out);
}

func (s StringSource) RuneAt(i int) rune {
	//FIXME this is horrible
	for si,r:=range s.source {
		if(si==i){
			return r;
		}
	}
	return rune(0);
}
  
func (s StringSource) Where(p int) string {
	if (p>15){
		return "After '... " + s.At(p-15,p) + "'";
	} else if (p>0) {
		return "After '" + s.At(0,p) + "'";
	} else {
		return "At start";
	}
}

func NewFileSource(filename string) (Source,error) {
	b,e:=ioutil.ReadFile(filename);
	if(e!=nil){
		return nil,e;
	}
	ss:=StringSource{string(b)};
	return &ss,nil;
}

