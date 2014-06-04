topLevelGroupings = [
  {name: "Priority", alloc: [
    {cnt:57},
    {name:"High"  ,id:1,cnt:20},
    {name:"Medium",id:2,cnt:17},
    {name:"Low"   ,id:3,cnt: 4},
  ]},
  {name: "Version", alloc: [
    {cnt:57+20+17+4-63},
    {name:"v2.x"  ,id:20,cnt:60},
    {name:"v3.x"  ,id:21,cnt:0},
    {name:"Defer" ,id:22,cnt:3},
  ]},
];

angular
  .module("myApp", [])
  .controller("myCtrl", function ($scope) {

    $scope.groupings = topLevelGroupings;

  })
;

